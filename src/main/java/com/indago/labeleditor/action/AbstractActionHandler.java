package com.indago.labeleditor.action;

import com.indago.labeleditor.display.RenderingManager;
import com.indago.labeleditor.model.LabelEditorModel;
import com.indago.labeleditor.model.LabelEditorTag;
import net.imglib2.roi.labeling.LabelingType;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractActionHandler<L> implements ActionHandler<L> {

	protected final LabelEditorModel<L> model;
	protected final RenderingManager<L> renderer;
	protected LabelingType<L> currentLabels;
	protected int currentSegment = -1;
	protected boolean mode3D;

	public AbstractActionHandler(LabelEditorModel<L> model, RenderingManager<L> renderer) {
		this.model = model;
		this.renderer = renderer;
	}

	public abstract void init();

	protected void handleMouseMove(MouseEvent e) {
		LabelingType<L> labels = getLabelsAtMousePosition(e);
		int intIndex;
		try {
			intIndex = labels.getIndex().getInteger();
		} catch(ArrayIndexOutOfBoundsException exc) {
			defocusAll();
			return;
		}
		if(intIndex == currentSegment) return;
		currentSegment = intIndex;
		new Thread(() -> {
			defocusAll();
			currentLabels = labels;
			labels.forEach(this::focus);
			updateLabelRendering();
		}).start();
	}

	protected void handleClick() {
		if (noLabelsAtMousePosition()) {
			deselectAll();
		} else {
			selectFirst(currentLabels);
		}
		updateLabelRendering();
	}

	protected void handleShiftClick() {
		if (!noLabelsAtMousePosition()) {
			addFirstToSelection(currentLabels);
		}
		updateLabelRendering();
	}
	protected abstract void updateLabelRendering();

	protected boolean noLabelsAtMousePosition() {
		return currentLabels == null || currentLabels.size() == 0;
	}

	protected void handleWheelRotation(double direction, boolean isHorizontal) {
		if(noLabelsAtMousePosition()) return;
		if(!anySelected(currentLabels)) {
			selectFirst(currentLabels);
			updateLabelRendering();
		}
		if ( !isHorizontal ) {
			if (direction > 0)
				selectNext(currentLabels);
			else
				selectPrevious(currentLabels);
			updateLabelRendering();
		}
	}

	@Override
	public abstract LabelingType<L> getLabelsAtMousePosition(MouseEvent e);

	@Override
	public void set3DViewMode(boolean mode3D) {
		this.mode3D = mode3D;
	}

	protected void selectFirst(LabelingType<L> currentLabels) {
		L label = getFirst(currentLabels);
		if(model.tagging().getTags(label).contains(LabelEditorTag.SELECTED)) return;
		deselectAll();
		select(label);
	}

	protected void addFirstToSelection(LabelingType<L> currentLabels) {
		L label = getFirst(currentLabels);
		if(model.tagging().getTags(label).contains(LabelEditorTag.SELECTED)) return;
		select(label);
	}

	private L getFirst(LabelingType<L> currentLabels) {
		List<L> orderedLabels = new ArrayList<>(currentLabels);
		orderedLabels.sort(model.getLabelComparator()::compare);
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

	protected void deselectAll() {
		model.tagging().removeTag(LabelEditorTag.SELECTED);
	}

	protected void defocusAll() {
		model.tagging().removeTag(LabelEditorTag.MOUSE_OVER);
		currentLabels = null;
	}

	protected void focus(L label) {
		model.tagging().addTag(LabelEditorTag.MOUSE_OVER, label);
	}
}
