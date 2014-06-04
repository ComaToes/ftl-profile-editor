package net.blerf.ftl.ui;

import net.blerf.ftl.ui.SpriteReference;


/**
 * A sprite which represents a SpriteReference's nested object.
 */
public interface ReferenceSprite<T> {
	public SpriteReference<T> getReference();

	public void referenceChanged();
}
