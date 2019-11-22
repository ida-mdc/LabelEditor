package sc.fiji.labeleditor.core.controller;

import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.core.model.tagging.TagChangedEvent;
import sc.fiji.labeleditor.core.view.LabelEditorView;
import sc.fiji.labeleditor.core.view.ViewChangedEvent;
import net.imglib2.roi.labeling.LabelingType;
import org.scijava.ui.behaviour.util.Behaviours;

import java.awt.*;
import java.util.List;

public interface LabelEditorInterface<L> {
	LabelingType<L> getLabelsAtMousePosition();
	LabelingType<L> findLabelsAtMousePosition(int x, int y, LabelEditorModel<L> model);
	void set3DViewMode(boolean mode3D);
	void installBehaviours(LabelEditorModel<L> model, LabelEditorController<L> controller, LabelEditorView<L> view);
	void onViewChange(ViewChangedEvent viewChangedEvent);

	Behaviours behaviours();

	Component getComponent();

	void onTagChange(List<TagChangedEvent> tagChangedEvents);
}