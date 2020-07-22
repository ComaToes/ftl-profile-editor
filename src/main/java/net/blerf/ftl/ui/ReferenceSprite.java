package net.blerf.ftl.ui;

import net.blerf.ftl.ui.SpriteReference;


/**
 * A sprite which represents a SpriteReference's nested object.
 */
public interface ReferenceSprite<T> {

	SpriteReference<T> getReference();

	void referenceChanged();
}
