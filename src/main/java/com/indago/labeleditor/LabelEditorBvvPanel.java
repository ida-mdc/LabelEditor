package com.indago.labeleditor;

import bvv.util.Bvv;
import bvv.util.BvvFunctions;
import bvv.util.BvvHandle;
import bvv.util.BvvSource;
import bvv.util.BvvStackSource;
import com.indago.labeleditor.action.ActionHandler;
import com.indago.labeleditor.action.DefaultBvvActionHandler;
import com.indago.labeleditor.display.DefaultLabelEditorRenderer;
import com.indago.labeleditor.display.LabelEditorRenderer;
import com.indago.labeleditor.model.DefaultLabelEditorModel;
import com.indago.labeleditor.model.LabelEditorModel;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.IntType;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class LabelEditorBvvPanel<L, T extends NumericType<T>> extends JPanel {

	private ImgPlus<T> data;
	private BvvHandle bvvHandle;
	private List< BvvSource > bdvSources = new ArrayList<>();

	private LabelEditorModel<L> model;
	private LabelEditorRenderer<L> renderer;
	private ActionHandler<L> actionHandler;

	private boolean panelBuilt = false;
	private boolean mode3D = false;

	public LabelEditorBvvPanel() {
	}

	public LabelEditorBvvPanel(ImgPlus<T> data) {
		setData(data);
		buildPanel();
	}

	public LabelEditorBvvPanel(ImgLabeling<L, IntType > labels) {
		init(labels);
	}

	public LabelEditorBvvPanel(ImgPlus< T > data, ImgLabeling<L, IntType > labels) {
		init(data, labels);
	}

	public LabelEditorBvvPanel(LabelEditorModel<L> model) {
		init(model);
	}

	public LabelEditorBvvPanel(ImgPlus<T> data, LabelEditorModel<L> model) {
		setData(data);
		init(model);
	}

	public void init(ImgPlus<T> data, ImgLabeling<L, IntType> labels) {
		setData(data);
		init(labels);
	}

	public void init(ImgLabeling<L, IntType> labels) {
		init(new DefaultLabelEditorModel<>(labels));
	}

	public void init(LabelEditorModel<L> model) {
		if(model != null) {
			this.model = model;
			actionHandler = initActionHandler(model);
			renderer = initRenderer(model);
		}
		buildPanel();
	}

	private void setData(ImgPlus<T> data) {
		this.data = data;
		if(data.dimensionIndex(Axes.Z) > 0) {
			mode3D = true;
		}
	}

	private void buildPanel() {
		if(panelBuilt) return;
		panelBuilt = true;
		//this limits the BDV navigation to 2D
		setLayout( new BorderLayout() );
		final JPanel viewer = new JPanel( new MigLayout("fill, w 500, h 500") );
		System.out.println("3D mode");
		BvvStackSource<ARGBType> source1 = BvvFunctions.show(fakeImg(), "", Bvv.options());
		bvvHandle = source1.getBvvHandle();
		viewer.add(bvvHandle.getViewerPanel(), "span, grow, push" );
		this.add( viewer );
		populateBvv();
		if(actionHandler != null) actionHandler.init();
	}

	private ImgPlus<ARGBType> fakeImg() {
		return new ImgPlus<>(new ArrayImgFactory<>(new ARGBType()).create(data.dimension(0), data.dimension(1)));
	}

	protected ActionHandler<L> initActionHandler(LabelEditorModel<L> model) {
		return new DefaultBvvActionHandler<L>(this, model);
	}

	protected LabelEditorRenderer<L> initRenderer(LabelEditorModel<L> model) {
		return new DefaultLabelEditorRenderer<L>(model);
	}

	private void populateBvv() {
		bvvRemoveAll();
		if(data != null) {
//			displayInBdv( data, "RAW" );
		}
		if(renderer == null) return;
		RandomAccessibleInterval<ARGBType> labelColorImg = renderer.getRenderedLabels();

		//TODO make virtual channels work
//		List<LUTChannel> virtualChannels = renderer.getVirtualChannels();
//		if(virtualChannels != null) {
//			List<BdvVirtualChannelSource> sources = BdvFunctions.show(
//					labelColorImg,
//					virtualChannels,
//					"solution",
//					Bdv.options().addTo(bdvGetHandlePanel()));
//			final Bdv bdv = sources.get( 0 );
//			for (int i = 0; i < virtualChannels.size(); ++i ) {
//				virtualChannels.get( i ).setPlaceHolderOverlayInfo( sources.get( i ).getPlaceHolderOverlayInfo() );
//				virtualChannels.get( i ).setViewerPanel( bdv.getBdvHandle().getViewerPanel() );
//			}
//		} else {
			BvvFunctions.show(
					labelColorImg,
					"solution",
					Bvv.options().addTo(bvvHandle));
//		}
	}

	private void displayInBdv( final RandomAccessibleInterval< T > img,
			final String title ) {
		final BvvSource source = BvvFunctions.show(
				img,
				title,
				Bvv.options().addTo(bvvHandle) );
		bvvGetSources().add( source );
		source.setActive( true );
	}

	private void bvvRemoveAll() {
		for ( final BvvSource source : bvvGetSources()) {
			source.removeFromBdv();
		}
		bvvGetSources().clear();
	}

	public BvvHandle getBvvHandle() {
		return bvvHandle;
	}

	public List< BvvSource > bvvGetSources() {
		return bdvSources;
	}

	public synchronized void updateLabelRendering() {
		renderer.update();
		bvvHandle.getViewerPanel().requestRepaint();
	}

	public LabelEditorRenderer<L> getRenderer() {
		return renderer;
	}

	public LabelEditorModel<L> getModel() {
		return model;
	}

	public ActionHandler<L> getActionHandler() {
		return actionHandler;
	}
}
