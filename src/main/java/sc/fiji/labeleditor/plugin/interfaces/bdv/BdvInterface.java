package sc.fiji.labeleditor.plugin.interfaces.bdv;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvHandle;
import bdv.util.BdvSource;
import bdv.viewer.ViewerPanel;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.util.Intervals;
import org.scijava.Context;
import org.scijava.plugin.Parameter;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.labeleditor.core.controller.DefaultInteractiveLabeling;
import sc.fiji.labeleditor.core.controller.InteractiveLabeling;
import sc.fiji.labeleditor.core.controller.LabelEditorBehaviours;
import sc.fiji.labeleditor.core.controller.LabelEditorInterface;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.core.model.tagging.TagChangedEvent;
import sc.fiji.labeleditor.core.view.DefaultLabelEditorView;
import sc.fiji.labeleditor.core.view.LabelEditorRenderer;
import sc.fiji.labeleditor.core.view.LabelEditorView;
import sc.fiji.labeleditor.core.view.ViewChangedEvent;
import sc.fiji.labeleditor.plugin.behaviours.FocusBehaviours;
import sc.fiji.labeleditor.plugin.behaviours.PopupBehaviours;
import sc.fiji.labeleditor.plugin.behaviours.modification.LabelingModificationBehaviours;
import sc.fiji.labeleditor.plugin.behaviours.select.SelectionBehaviours;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BdvInterface implements LabelEditorInterface {

	@Parameter
	private Context context;

	private final BdvHandle bdvHandle;
	private final Behaviours behaviours;

	private final Map<LabelEditorView<?>, List<BdvSource>> sources = new HashMap<>();

	public BdvInterface(BdvHandle bdvHandle) {
		this.bdvHandle = bdvHandle;
		this.behaviours = new Behaviours(new InputTriggerConfig(), "labeleditor");
		behaviours.install(this.bdvHandle.getTriggerbindings(), "labeleditor");
	}

	public static <L> InteractiveLabeling<L> control(LabelEditorModel<L> model, LabelEditorView<L> view, BdvHandle bdvHandle, Context context) {
		BdvInterface interfaceInstance = new BdvInterface(bdvHandle);
		DefaultInteractiveLabeling<L> interactiveLabeling = new DefaultInteractiveLabeling<>(model, view, interfaceInstance);
		if(context != null) context.inject(interactiveLabeling);
		interactiveLabeling.initialize();
		return interactiveLabeling;
	}

	public static <L> InteractiveLabeling<L> control(LabelEditorModel<L> model, BdvHandle handle, Context context) {
		LabelEditorView<L> view = new DefaultLabelEditorView<>(model);
		if(context != null) context.inject(view);
		view.addDefaultRenderers();
		return control(model, view, handle, context);
	}

	public <L> LabelingType<L> findLabelsAtMousePosition(int x, int y, InteractiveLabeling<L> labeling) {
		RandomAccess<LabelingType<L>> ra = labeling.getLabelingInScope().randomAccess();
		Localizable pos = getDataPositionAtMouse();
		if(Intervals.contains(labeling.getLabelingInScope(), pos)) {
			ra.setPosition(pos);
			//FIXME
//			bdvHandle.getViewerPanel().getDisplay().setToolTipText(view.getToolTip(labelsAtCursor));
			return ra.get();
		}
		//FIXME
//		bdvHandle.getViewerPanel().getDisplay().setToolTipText(null);
		return null;
	}

	private Localizable getDataPositionAtMouse() {
		//FIXME currently only works for 2D, 3D and 4D
		RealPoint mousePointer = new RealPoint(3);
		bdvHandle.getViewerPanel().getGlobalMouseCoordinates( mousePointer );
		final int x = (int) Math.round( mousePointer.getDoublePosition( 0 ) );
		final int y = (int) Math.round( mousePointer.getDoublePosition( 1 ) );
		final int z = (int) Math.round( mousePointer.getDoublePosition( 2 ) );
		int time = bdvHandle.getViewerPanel().getState().getCurrentTimepoint();
		return new Point(x, y, z, time);
	}

	@Override
	public <L> void installBehaviours(InteractiveLabeling<L> labeling) {
		((LabelEditorBehaviours) labeling.model().getSelectionModel()).init(labeling);
		((LabelEditorBehaviours) labeling.model().getSelectionModel()).install(behaviours, bdvHandle.getViewerPanel().getDisplay());
		install(labeling, new FocusBehaviours<>());
		install(labeling, new LabelingModificationBehaviours<>());
		install(labeling, new PopupBehaviours<>());
	}

	private <L> void install(InteractiveLabeling<L> labeling, LabelEditorBehaviours<L> behavioursAdded) {
		if(context != null) context.inject(behavioursAdded);
		behavioursAdded.init(labeling);
		behavioursAdded.install(behaviours, bdvHandle.getViewerPanel().getDisplay());
	}

	@Override
	public <L> void install(LabelEditorBehaviours<L> behaviour, InteractiveLabeling<L> labeling) {
		if(context != null) context.inject(behaviour);
		behaviour.init(labeling);
		behaviour.install(behaviours(), getComponent());
	}

	@Override
	public void onViewChange(ViewChangedEvent viewChangedEvent) {
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public Behaviours behaviours() {
		return behaviours;
	}

	@Override
	public ViewerPanel getComponent() {
		return bdvHandle.getViewerPanel();
	}

	@Override
	public void onTagChange(List<TagChangedEvent> tagChangedEvents) {
		//FIXME
//		if(labelsAtCursor == null) return;
//		bdvHandle.getViewerPanel().getDisplay().setToolTipText(view.getToolTip(labelsAtCursor));
//		showToolTip(bdvHandle.getViewerPanel().getDisplay());
	}

	@Override
	public <L> void display(LabelEditorView<L> view) {
		ArrayList<BdvSource> sources = new ArrayList<>();
		List<LabelEditorRenderer<L>> renderers = new ArrayList<>(view.renderers());
		Collections.reverse(renderers);
		renderers.forEach(renderer -> sources.add(display(renderer.getOutput(), renderer.getName())));
		this.sources.put(view, sources);
	}

	private BdvSource display(RandomAccessibleInterval rai, String name) {
		if(rai == null) return null;
		final BdvSource source = BdvFunctions.show(rai, name, Bdv.options().addTo(bdvHandle));
		source.setActive(true);
		return source;
	}

	public static void showToolTip(JComponent component) {
		java.awt.Point locationOnScreen = MouseInfo.getPointerInfo().getLocation();
		java.awt.Point locationOnComponent = new java.awt.Point(locationOnScreen);
		SwingUtilities.convertPointFromScreen(locationOnComponent, component);
		if (component.contains(locationOnComponent)) {
			ToolTipManager.sharedInstance().mouseMoved(
					new MouseEvent(component, -1, System.currentTimeMillis(), 0, locationOnComponent.x, locationOnComponent.y,
							locationOnScreen.x, locationOnScreen.y, 0, false, 0));
		}
	}

	public Map<LabelEditorView<?>, List<BdvSource>> getSources() {
		return sources;
	}
}
