package com.indago.labeleditor.plugin.behaviours.modification;

import com.indago.labeleditor.application.InteractiveWatershedCommand;
import com.indago.labeleditor.core.controller.LabelEditorController;
import com.indago.labeleditor.core.model.LabelEditorModel;
import com.indago.labeleditor.core.model.tagging.LabelEditorTag;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.IntArray;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.UIService;
import org.scijava.ui.behaviour.Behaviour;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class SplitLabels<L> implements Behaviour {

	private final LabelEditorController controller;
	private final LabelEditorModel<L> model;
	@Parameter
	private OpService opService;
	@Parameter
	private CommandService commandService;
	@Parameter
	private UIService uiService;

	public SplitLabels(LabelEditorModel model, LabelEditorController controller) {
		this.model = model;
		this.controller = controller;
	}

	public void splitSelected() {
		if(opService == null) {
			throw new RuntimeException("No OpService available. You have to inject your LabelEditorPanel with a context to use this behaviour.");
		}
		Set<L> selected = model.tagging().getLabels(LabelEditorTag.SELECTED);
		selected.forEach(label -> {
			try {
				splitInteractively(label);
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
			}
		});
		controller.triggerLabelingChange();
	}

	public <T extends NativeType<T>> void splitInteractively(L label) throws ExecutionException, InterruptedException {
		LabelRegions regions = new LabelRegions<>(model.labels());
		LabelRegion<Integer> region = regions.getLabelRegion(label);
		IntervalView<BoolType> zeroRegion = Views.zeroMin(region);
		ImgLabeling<L, IntType> cropLabeling = createCroppedLabeling(label, region);
		ImgPlus data = createCroppedData(region, zeroRegion);
		CommandModule out = commandService.run(
				InteractiveWatershedCommand.class, true,
				"labeling", cropLabeling,
				"data", data).get();
		LabelEditorModel outModel = (LabelEditorModel) out.getOutput("output");
		if(outModel != null) {
			System.out.println("new labels: " + outModel.labels().getMapping().getLabels().size());
//			TODO add new labels to model labeling
//			Set<Object> tags = model.tagging().getTags(label);
//			newLabeling.forEach(newlabel -> tags.forEach(tag -> model.tagging().addTag(tag, newlabel)));
			controller.triggerLabelingChange();
		}


//		ImgLabeling< Integer, IntType > watershed = new ImgLabeling<>( backing );
//
//		IntervalView dataCrop = Views.zeroMin(Views.interval(data, region));
//		RandomAccessibleInterval gaussCrop = opService.filter().gauss(dataCrop, sigma);
//		final ImgLabeling<Integer, IntType> seeds = findAndDisplayLocalMaxima(gaussCrop);
//		Img invertedDataCrop = opService.create().img(Views.iterable(dataCrop));
//		Set<L> newlabels = split(label, model.labels(), model.getData(), 1, opService);
	}

	private <T extends NativeType<T>> ImgPlus createCroppedData(LabelRegion<Integer> region, IntervalView<BoolType> zeroRegion) {
		Img<T> dataImg = opService.create().img(zeroRegion, (T) model.getData().firstElement());
		Cursor<T> dataInCursor = Views.zeroMin(Views.interval(model.getData(), region)).localizingCursor();
		RandomAccess<T> dataOutRA = dataImg.randomAccess();
		while(dataInCursor.hasNext()) {
			T val = dataInCursor.next();
			dataOutRA.setPosition(dataInCursor);
			dataOutRA.get().set(val);
		}
		return new ImgPlus(dataImg);
	}

	private ImgLabeling<L, IntType> createCroppedLabeling(L label, LabelRegion<Integer> region) {
		Img<IntType> backing = new ArrayImgFactory<>(new IntType()).create( Views.zeroMin(region) );
		ImgLabeling<L, IntType> cropLabeling = new ImgLabeling<>(backing);
		Point offset = new Point(region.numDimensions());
		for (int i = 0; i < region.numDimensions(); i++) {
			offset.setPosition(-region.min(i), i);
		}
		LabelRegionCursor inCursor = region.localizingCursor();
		RandomAccess<LabelingType<L>> outRA = cropLabeling.randomAccess();
		while(inCursor.hasNext()) {
			inCursor.next();
			outRA.setPosition(inCursor);
			outRA.move(offset);
			outRA.get().add(label);
		}
		return cropLabeling;
	}

	public static <L> Set<L> split(L label, ImgLabeling<L, IntType> labeling, RandomAccessibleInterval data, double sigma, OpService opService) {
		LabelRegions regions = new LabelRegions<>(labeling);
		LabelRegion<Integer> region = regions.getLabelRegion(label);
		Img<BitType> mask = createMask(opService, region);
		ImgLabeling<Integer, IntType> watershed = createWatershedResultLabeling(region);
		Img watershedInput = createWatershedInput(data, sigma, opService, region);
		final ImgLabeling<Integer, IntType> seeds = findAndDisplayLocalMinima(watershedInput, mask);
		opService.image().watershed(watershed, watershedInput, seeds, true, false, mask);

		//writing the watershedded values back into the original labeling, with the integer offset to not have duplicate labels
		IntervalView<LabelingType<L>> labelingCrop = Views.zeroMin(Views.interval(labeling, region));
		Cursor<LabelingType<Integer>> waterShedCursor = watershed.localizingCursor();
		RandomAccess<LabelingType<L>> labelingRa = labelingCrop.randomAccess();
		int startVal = labeling.getMapping().getLabels().size();
		Set<L> res = new HashSet<>();
		while(waterShedCursor.hasNext()) {
			LabelingType<Integer> vals = waterShedCursor.next();
			labelingRa.setPosition(waterShedCursor);
			vals.forEach(val -> {
				L newlabel = (L) new Integer(val + startVal);
				res.add(newlabel);
				labelingRa.get().add(newlabel);
			});
		}
		DeleteLabels.delete(label, labeling);
		return res;
	}

	private static Img<BitType> createMask(OpService opService, LabelRegion<Integer> region) {
		Img<BitType> mask = opService.create().img(Views.zeroMin(region), new BitType());
		Point offset = new Point(region.numDimensions());
		for (int i = 0; i < region.numDimensions(); i++) {
			offset.setPosition(-region.min(i), i);
		}
		LabelRegionCursor regionCursor = region.localizingCursor();
		RandomAccess<BitType> maskRA = mask.randomAccess();
		while(regionCursor.hasNext()) {
			regionCursor.next();
			maskRA.setPosition(regionCursor);
			maskRA.move(offset);
			maskRA.get().setOne();
		}
		return mask;
	}

	private static Img createWatershedInput(RandomAccessibleInterval data, double sigma, OpService opService, LabelRegion<Integer> region) {
		IntervalView dataCrop = Views.zeroMin(Views.interval(data, region));
		RandomAccessibleInterval gaussCrop = opService.filter().gauss(dataCrop, sigma);
		Img invertedDataCrop = opService.create().img(Views.iterable(dataCrop));
		opService.image().invert(invertedDataCrop, Views.iterable(gaussCrop));
		return invertedDataCrop;
	}

	private static ImgLabeling<Integer, IntType> createWatershedResultLabeling(LabelRegion<Integer> region) {
		ArrayImg<IntType, IntArray> backing = (ArrayImg<IntType, IntArray>) new ArrayImgFactory<>(new IntType()).create( region );
		return new ImgLabeling<>( backing );
	}


	public static < T extends Comparable< T >> ImgLabeling<Integer, IntType>
	findAndDisplayLocalMinima(RandomAccessibleInterval<T> source, Img<BitType> mask) {
		ArrayImg<IntType, IntArray> backing = (ArrayImg<IntType, IntArray>) new ArrayImgFactory<>(new IntType()).create( source );
		ImgLabeling< Integer, IntType > res = new ImgLabeling<>( backing );
		RandomAccess<LabelingType<Integer>> ra = res.randomAccess();
		int count = 0;
		Interval interval = Intervals.expand( source, -1 );
		source = Views.interval( source, interval );
		final Cursor< T > center = Views.iterable( source ).cursor();
		RandomAccess<BitType> maskRA = mask.randomAccess();
		final RectangleShape shape = new RectangleShape( 1, true );
		for ( final Neighborhood< T > localNeighborhood : shape.neighborhoods( source ) ) {
			final T centerValue = center.next();
			maskRA.setPosition(center);
			if(maskRA.get().get()) {
				boolean isMinimum = true;
				for ( final T value : localNeighborhood ) {
					if ( centerValue.compareTo( value ) >= 0 ) {
						isMinimum = false;
						break;
					}
				}
				if ( isMinimum ) {
					ra.setPosition(center);
					ra.get().add(count++);
				}
			}
		}
		return res;
	}

}