package net.blerf.ftl.ui;

import java.util.ArrayList;
import java.util.List;

import net.blerf.ftl.ui.ReferenceSprite;


/**
 * A reference to a shared object, to be represented by multiple sprites.
 */
public class SpriteReference<T> {

	private List<ReferenceSprite<T>> spriteList = new ArrayList<ReferenceSprite<T>>(1);
	private T referent;


	/**
	 * Constructor.
	 *
	 * @param a shared object, or null (where permitted)
	 */
	public SpriteReference( T referent ) {
		this.referent = referent;
	}

	/**
	 * Sets a new referent.
	 */
	public void set( T newReferent ) {
		referent = newReferent;
	}

	/**
	 * Returns the referent.
	 */
	public T get() {
		return referent;
	}

	/**
	 * Registers a sprite to notify of changes.
	 */
	public void addSprite( ReferenceSprite<T> sprite ) {
		spriteList.add( sprite );
	}

	public void removeSprite( ReferenceSprite<T> sprite ) {
		spriteList.remove( sprite );
	}

	/**
	 * Returns the first sprite of a given class, or null.
	 */
	public <U> U getSprite( Class<U> spriteClass ) {
		for ( ReferenceSprite<T> sprite : spriteList ) {
			if ( spriteClass.isInstance( sprite ) ) {
				return spriteClass.cast( sprite );
			}
		}
		return null;
	}

	/**
	 * Notifies all sprites that changes have been made to the referent.
	 */
	public void fireReferenceChange() {
		for ( ReferenceSprite<T> sprite : spriteList ) {
			sprite.referenceChanged();
		}
	}
}
