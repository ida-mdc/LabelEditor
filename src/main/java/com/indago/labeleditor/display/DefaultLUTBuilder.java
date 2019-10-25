package com.indago.labeleditor.display;

import com.indago.labeleditor.model.LabelEditorModel;
import com.indago.labeleditor.model.LabelEditorTag;
import net.imglib2.type.numeric.ARGBType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultLUTBuilder<U> implements LUTBuilder<U> {

	private static int colorMouseOver = ARGBType.rgba(50,50,50,100);
	private static int colorSelected = ARGBType.rgba(255,50,50,100);
	private final Map<Object, LUTChannel> tagColors;

	public DefaultLUTBuilder() {
		tagColors = new HashMap<>();
		tagColors.put(LabelEditorTag.SELECTED, new LUTChannel(colorSelected));
		tagColors.put(LabelEditorTag.MOUSE_OVER, new LUTChannel(colorMouseOver));
	}

	@Override
	public int[] build(LabelEditorModel<U> model) {

		if(model.getLabels() == null) return new int[0];

		int[] lut;

		// our LUT has one entry per index in the index img of our labeling
		lut = new int[model.getLabels().getMapping().numSets()];

		for (int i = 0; i < lut.length; i++) {
			// get all labels of this index
			Set<U> labels = model.getLabels().getMapping().labelsAtIndex(i);

			// if there are no labels, we don't need to check for tags and can continue
			if(labels.size() == 0) continue;

			// get all tags associated with the labels of this index
			Set<Object> mytags = filterTagsByLabels(model.getTags(), labels);

			lut[i] = mixColors(mytags, tagColors);

		}

		return lut;
	}

	//https://en.wikipedia.org/wiki/Alpha_compositing
	//https://wikimedia.org/api/rest_v1/media/math/render/svg/12ea004023a1756851fc7caa0351416d2ba03bae
	static int mixColors(Set<Object> mytags, Map<Object, LUTChannel> tagColors) {
		float red = 0;
		float green = 0;
		float blue = 0;
		float alpha = 0;
		for (Object tag : mytags) {
			LUTChannel lutChannel = tagColors.get(tag);
			if(lutChannel == null) continue;
			int color = lutChannel.getColor();
			float newred = ARGBType.red(color);
			float newgreen = ARGBType.blue(color);
			float newblue = ARGBType.green(color);
			float newalpha = ((float)ARGBType.alpha(color))/255.f;
			red = (red*alpha+newred*newalpha*(1-alpha))/(alpha + newalpha*(1-alpha));
			green = (green*alpha+newgreen*newalpha*(1-alpha))/(alpha + newalpha*(1-alpha));
			blue = (blue*alpha+newblue*newalpha*(1-alpha))/(alpha + newalpha*(1-alpha));
			alpha = alpha + newalpha*(1-alpha);
		}
		return ARGBType.rgba((int)red, (int)green, (int)blue, (int)(alpha*255));
	}

	@Override
	public List<LUTChannel> getVirtualChannels() {
		return new ArrayList<>(tagColors.values());
	}

	@Override
	public void setColor(Object tag, int color) {
		tagColors.put(tag, new LUTChannel(color));
	}

	@Override
	public void removeColor(Object tag) {
		tagColors.remove(tag);
	}

	private Set<Object> filterTagsByLabels(Map<U, Set<Object>> tags, Set<U> labels) {
		return tags.entrySet().stream().filter(entry -> labels.contains(entry.getKey())).map(Map.Entry::getValue).flatMap(Set::stream).collect(Collectors.toSet());
	}

}
