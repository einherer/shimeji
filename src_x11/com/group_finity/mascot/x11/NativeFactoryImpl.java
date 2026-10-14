package com.group_finity.mascot.x11;

import java.awt.image.BufferedImage;

import com.group_finity.mascot.NativeFactory;
import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;

/**
 * Ursprünglicher Autor: Yuki Yamada von Group Finity (http://www.group-finity.com/Shimeji/)
 * Wird derzeit von der Shimeji-ee Group entwickelt.
 */

public class NativeFactoryImpl extends NativeFactory {
	
	private Environment environment = new X11Environment();
	
	@Override
	public Environment getEnvironment() {
		return this.environment;
	}
	
	@Override
	public NativeImage newNativeImage(final BufferedImage src) {
		return new X11NativeImage(src);
	}
	
	@Override
	public TranslucentWindow newTransparentWindow() {
		return new X11TranslucentWindow();
	}

}
