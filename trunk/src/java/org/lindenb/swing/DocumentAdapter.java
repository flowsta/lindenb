package org.lindenb.swing;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public abstract class DocumentAdapter implements DocumentListener {

	@Override
	public void changedUpdate(DocumentEvent e)
		{
		documentChanged(e);
		}

	@Override
	public void insertUpdate(DocumentEvent e)
		{
		documentChanged(e);
		}

	@Override
	public void removeUpdate(DocumentEvent e)
		{
		documentChanged(e);
		}
	
	public abstract void documentChanged(DocumentEvent e);
	}