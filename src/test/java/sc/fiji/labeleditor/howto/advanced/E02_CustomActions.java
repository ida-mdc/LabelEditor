package sc.fiji.labeleditor.howto.advanced;

import bdv.util.BdvOptions;
import sc.fiji.labeleditor.core.controller.InteractiveLabeling;
import sc.fiji.labeleditor.core.model.DefaultLabelEditorModel;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.core.model.tagging.LabelEditorTag;
import sc.fiji.labeleditor.plugin.interfaces.bdv.LabelEditorBdvPanel;
import net.imagej.ImageJ;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.integer.IntType;
import org.scijava.ui.behaviour.ClickBehaviour;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * How to add custom behaviours to the LabelEditor
 */
public class E02_CustomActions {

	/**
	 * In this example, the default selection and focus colors are removed from the model.
	 * One can toggle a special tag of a label where the mouse is currently hovering by pressing L on the keyboard.
	 */
	public void mouseAction() throws IOException {
		ImageJ ij = new ImageJ();
		ij.launch();

		//open blobs
		Img input = (Img) ij.io().open(getClass().getResource("/blobs.png").getPath());

		//do connected component analysis
		Img binary = (Img) ij.op().threshold().otsu(input);
		ImgLabeling<Integer, IntType> labeling = ij.op().labeling().cca(binary, ConnectedComponents.StructuringElement.EIGHT_CONNECTED);

		//create model
		LabelEditorModel<Integer> model = new DefaultLabelEditorModel<>(labeling, input);

		//set colors
		model.colors().getColorset(LabelEditorTag.SELECTED).clear();
		model.colors().getColorset(LabelEditorTag.MOUSE_OVER).clear();
		model.colors().getFaceColor("special").set(255, 0, 0);

		LabelEditorBdvPanel panel = new LabelEditorBdvPanel(ij.context(), new BdvOptions().is2D());
		InteractiveLabeling<Integer> interactiveLabeling = panel.add(model);

//		panel.getSources().forEach(source -> source.setDisplayRange(0, 100));

		interactiveLabeling.interfaceInstance().behaviours().behaviour((ClickBehaviour) (x, y) -> {

				//get labels at current mouse position
				LabelingType<Integer> labels = interactiveLabeling.interfaceInstance().findLabelsAtMousePosition(x, y, interactiveLabeling);

				//pausing the tagging listeners while changing tags improves performance
				model.tagging().pauseListeners();
				// for all labels at the current mouse position, toggle special tag
				for (Integer label : labels) {
					model.tagging().toggleTag("special", label);
				}
				model.tagging().resumeListeners();
			},
		"add my special label","L" );

		//build frame
		JFrame frame = new JFrame("Label editor");
		frame.setContentPane(panel);
		frame.setMinimumSize(new Dimension(500,500));
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String...args) throws IOException {
		new E02_CustomActions().mouseAction();
	}

}
