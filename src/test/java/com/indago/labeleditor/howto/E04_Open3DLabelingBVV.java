package com.indago.labeleditor.howto;

import com.indago.labeleditor.core.LabelEditorPanel;
import com.indago.labeleditor.core.model.DefaultLabelEditorModel;
import com.indago.labeleditor.core.view.LabelEditorTargetComponent;
import com.indago.labeleditor.plugin.interfaces.bvv.LabelEditorBvvPanel;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.view.Views;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class E04_Open3DLabelingBVV {

	public <T extends RealType<T>> void run() throws IOException {
		ImageJ ij = new ImageJ();
		ij.launch();

		Img input = (Img) ij.io().open(getClass().getResource("/blobs.png").getPath());
		RandomAccessibleInterval inputStack = input;
		List<RandomAccessibleInterval<T>> stack = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			stack.add(inputStack);
		}
		inputStack = Views.stack(stack);
		Img thresholded = (Img) ij.op().threshold().otsu(Views.iterable(inputStack));
		ImgLabeling<Integer, IntType> labeling = ij.op().labeling().cca(thresholded, ConnectedComponents.StructuringElement.EIGHT_CONNECTED);

		DefaultLabelEditorModel<Integer> model = new DefaultLabelEditorModel<>();
		model.init(labeling);

		model.labels().getMapping().getLabels().forEach(label -> {
			model.tagging().addTag("displayed", label);
		});

		LabelEditorPanel<Integer> panel = new LabelEditorBvvPanel<>();
		ij.context().inject(panel);
		panel.init(model);
		panel.model().colors().get("displayed").put(LabelEditorTargetComponent.FACE, ARGBType.rgba(255,255,0,155));

		JFrame frame = new JFrame("Label editor");
		frame.setContentPane(panel.get());
		frame.setMinimumSize(new Dimension(500,500));
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String... args) throws IOException {
		new E04_Open3DLabelingBVV().run();
	}


}
