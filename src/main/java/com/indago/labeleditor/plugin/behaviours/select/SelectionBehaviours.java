package com.indago.labeleditor.plugin.behaviours.select;

import com.indago.labeleditor.core.controller.LabelEditorBehaviours;
import com.indago.labeleditor.core.controller.LabelEditorController;
import com.indago.labeleditor.core.model.LabelEditorModel;
import com.indago.labeleditor.core.model.tagging.LabelEditorTag;
import com.indago.labeleditor.core.view.LabelEditorView;
import net.imglib2.roi.labeling.LabelingType;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SelectionBehaviours<L> implements LabelEditorBehaviours<L> {

	@Parameter
	CommandService commandService;

	protected LabelEditorModel<L> model;
	protected LabelEditorController<L> controller;

	protected static final String TOGGLE_LABEL_SELECTION_NAME = "LABELEDITOR_TOGGLELABELSELECTION";
	protected static final String TOGGLE_LABEL_SELECTION_TRIGGERS = "shift scroll";
	protected static final String SELECT_FIRST_LABEL_NAME = "LABELEDITOR_SELECTFIRSTLABEL";
	protected static final String SELECT_FIRST_LABEL_TRIGGERS = "button1";
	protected static final String ADD_LABEL_TO_SELECTION_NAME = "LABELEDITOR_ADDLABELTOSELECTION";
	protected static final String ADD_LABEL_TO_SELECTION_TRIGGERS = "shift button1";
	protected static final String SELECT_ALL_LABELS_NAME = "LABELEDITOR_SELECTALL";
	protected static final String SELECT_ALL_LABELS_TRIGGERS = "ctrl A";

	@Override
	public void init(LabelEditorModel<L> model, LabelEditorController<L> controller, LabelEditorView<L> view) {
		this.model = model;
		this.controller = controller;
	}

	@Override
	public void install(Behaviours behaviours, Component panel) {

		behaviours.behaviour(getToggleLabelSelectionBehaviour(), TOGGLE_LABEL_SELECTION_NAME, TOGGLE_LABEL_SELECTION_TRIGGERS);
		behaviours.behaviour(getSelectFirstLabelBehaviour(), SELECT_FIRST_LABEL_NAME, SELECT_FIRST_LABEL_TRIGGERS);
		behaviours.behaviour(getAddFirstLabelToSelectionBehaviour(), ADD_LABEL_TO_SELECTION_NAME, ADD_LABEL_TO_SELECTION_TRIGGERS);
		behaviours.behaviour(getSelectAllBehaviour(), SELECT_ALL_LABELS_NAME, SELECT_ALL_LABELS_TRIGGERS);

	}

	private Behaviour getSelectAllBehaviour() {
		return (ClickBehaviour) (arg0, arg1) -> selectAll();
	}

	protected Behaviour getAddFirstLabelToSelectionBehaviour() {
		return (ClickBehaviour) (arg0, arg1) -> addFirstLabelToSelection(arg0, arg1);
	}

	protected Behaviour getSelectFirstLabelBehaviour() {
		return (ClickBehaviour) (arg0, arg1) -> selectFirstLabel(arg0, arg1);
	}

	protected Behaviour getToggleLabelSelectionBehaviour() {
		return (ScrollBehaviour) (wheelRotation, isHorizontal, x, y) -> {
			if(!isHorizontal) toggleLabelSelection(wheelRotation > 0, x, y);};
	}

	public void selectAll() {
		model.labels().getMapping().getLabels().forEach(this::select);
	}

	protected void selectFirstLabel(int x, int y) {
		LabelingType<L> labels = controller.interfaceInstance().findLabelsAtMousePosition(x, y, model);
		model.tagging().pauseListeners();
		if (foundLabels(labels)) {
			selectFirst(labels);
		} else {
			deselectAll();
		}
		model.tagging().resumeListeners();
	}

	private boolean foundLabels(LabelingType<L> labels) {
		return labels != null && labels.size() > 0;
	}

	protected void addFirstLabelToSelection(int x, int y) {
		LabelingType<L> labels = controller.interfaceInstance().findLabelsAtMousePosition(x, y, model);
		model.tagging().pauseListeners();
		if (foundLabels(labels)) {
			toggleSelectionOfFirst(labels);
		}
		model.tagging().resumeListeners();
	}

	protected void toggleLabelSelection(boolean forwardDirection, int x, int y) {
		LabelingType<L> labels = controller.interfaceInstance().findLabelsAtMousePosition(x, y, model);
		if(!foundLabels(labels)) return;
		if(!anySelected(labels)) {
			model.tagging().pauseListeners();
			selectFirst(labels);
			model.tagging().resumeListeners();
			return;
		}
		model.tagging().pauseListeners();
		if (forwardDirection)
			selectNext(labels);
		else
			selectPrevious(labels);
		model.tagging().resumeListeners();
	}

	protected void selectFirst(LabelingType<L> labels) {
		L label = getFirst(labels);
		if(model.tagging().getTags(label).contains(LabelEditorTag.SELECTED)) return;
		deselectAll();
		select(label);
	}

	protected void toggleSelectionOfFirst(LabelingType<L> labels) {
		L label = getFirst(labels);
		if(model.tagging().getTags(label).contains(LabelEditorTag.SELECTED)) {
			deselect(label);
		} else {
			select(label);
		}
	}

	protected L getFirst(LabelingType<L> labels) {
		if(labels.size() == 0) return null;
		List<L> orderedLabels = new ArrayList<>(labels);
		orderedLabels.sort(model.getLabelComparator());
		return orderedLabels.get(0);
	}

	protected boolean isSelected(L label) {
		return model.tagging().getTags(label).contains(LabelEditorTag.SELECTED);
	}

	protected boolean anySelected(LabelingType<L> labels) {
		return labels.stream().anyMatch(label -> model.tagging().getTags(label).contains(LabelEditorTag.SELECTED));
	}

	protected void select(L label) {
		model.tagging().addTag(LabelEditorTag.SELECTED, label);
	}

	protected void selectPrevious(LabelingType<L> labels) {
		List<L> reverseLabels = new ArrayList<>(labels);
		Collections.reverse(reverseLabels);
		selectNext(reverseLabels);
	}

	protected void selectNext(Collection<L> labels) {
		boolean foundSelected = false;
		for (Iterator<L> iterator = labels.iterator(); iterator.hasNext(); ) {
			L label = iterator.next();
			if (isSelected(label)) {
				foundSelected = true;
				if(iterator.hasNext()) {
					deselect(label);
				}
			} else {
				if (foundSelected) {
					select(label);
					return;
				}
			}
		}
	}

	protected void deselect(L label) {
		model.tagging().removeTag(LabelEditorTag.SELECTED, label);
	}

	public void deselectAll() {
		model.tagging().removeTag(LabelEditorTag.SELECTED);
	}

	public void invertSelection() {
		Set<L> selected = model.tagging().getLabels(LabelEditorTag.SELECTED);
		Set<L> all = new HashSet(model.labels().getMapping().getLabels());
		all.removeAll(selected);
		all.forEach(label -> select(label));
		selected.forEach(label -> deselect(label));
	}

	public void selectByTag() {
		commandService.run(SelectByTagCommand.class, true, "model", model);
	}
}