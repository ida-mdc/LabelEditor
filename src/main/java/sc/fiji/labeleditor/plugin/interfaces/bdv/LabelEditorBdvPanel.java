package sc.fiji.labeleditor.plugin.interfaces.bdv;

import bdv.util.Bdv;
import bdv.util.BdvHandlePanel;
import bdv.util.BdvOptions;
import bdv.util.BdvSource;
import net.miginfocom.swing.MigLayout;
import org.scijava.Context;
import org.scijava.Disposable;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import sc.fiji.labeleditor.core.controller.DefaultInteractiveLabeling;
import sc.fiji.labeleditor.core.controller.InteractiveLabeling;
import sc.fiji.labeleditor.core.model.LabelEditorModel;
import sc.fiji.labeleditor.core.view.DefaultLabelEditorView;
import sc.fiji.labeleditor.core.view.LabelEditorView;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LabelEditorBdvPanel extends JPanel implements Disposable {

	@Parameter
	private Context context;

	@Parameter
	private CommandService commandService;

	private final Map<String, InteractiveLabeling> labelings = new HashMap<>();

	private BdvHandlePanel bdvHandlePanel;
	private BdvInterface interfaceInstance;

	public LabelEditorBdvPanel() {
		this(null, Bdv.options());
	}

	public LabelEditorBdvPanel(Context context) {
		this(context, Bdv.options());
	}

	public LabelEditorBdvPanel(BdvOptions options) {
		this(null, options);
	}

	public LabelEditorBdvPanel(Context context, BdvOptions options) {
		if(context != null) context.inject(this);
		initialize(options);
	}

	private void initialize(BdvOptions options) {
		adjustOptions(options);
		setMinimumSize(new Dimension(100, 100));
		setPreferredSize(new Dimension(500, 500));
		setLayout( new MigLayout("fill") );
		bdvHandlePanel = new BdvHandlePanel(null, options);
		interfaceInstance = new BdvInterface(bdvHandlePanel.getBdvHandle(), context);
		Component viewer = bdvHandlePanel.getViewerPanel();
		this.add( viewer, "span, grow, push" );
	}

	protected void adjustOptions(BdvOptions options) {
		options.accumulateProjectorFactory(LabelEditorAccumulateProjector.factory);
	}

	public <L> InteractiveLabeling<L> add(LabelEditorModel<L> model) {
		return add(model, new BdvOptions());
	}

	public <L> InteractiveLabeling<L> add(LabelEditorModel<L> model, BdvOptions options) {
		LabelEditorView<L> view = new DefaultLabelEditorView<>(model);
		if(context != null) context.inject(view);
		view.addDefaultRenderers();
		return add(model, view, options);
	}

	public <L> InteractiveLabeling<L> add(LabelEditorModel<L> model, LabelEditorView<L> view) {
		return add(model, view, new BdvOptions());
	}

	public <L> InteractiveLabeling<L> add(LabelEditorModel<L> model, LabelEditorView<L> view, BdvOptions options) {
		return interfaceInstance.control(model, view, options);
	}

	@Override
	public void dispose() {
		if(bdvHandlePanel != null) bdvHandlePanel.close();
	}

	public List<BdvSource> getSources() {
		List<BdvSource> res = new ArrayList<>();
		labelings.forEach((name, labeling) -> {
			BdvInterface labelEditorInterface = (BdvInterface) labeling.interfaceInstance();
			labelEditorInterface.getSources().values().forEach(res::addAll);
		});
		return res;
	}

	protected Context context() {
		return context;
	}

	public BdvHandlePanel getBdvHandlePanel() {
		return bdvHandlePanel;
	}

	protected BdvInterface getInterfaceInstance() {
		return interfaceInstance;
	}
}
