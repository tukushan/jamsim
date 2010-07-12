package org.jamsim.ascape.navigator;

import java.io.IOException;

import javax.swing.JPopupMenu;

import org.ascape.runtime.swing.navigator.PanelViewNode;
import org.ascape.runtime.swing.navigator.PanelViewProvider;
import org.ascape.runtime.swing.navigator.PopupMenuProvider;
import org.ascape.view.vis.PanelView;
import org.jamsim.ascape.output.Saveable;

/**
 * A {@link Saveable} {@link PanelViewNode}.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public class SaveablePanelViewNode extends PanelViewNode implements Saveable,
		PopupMenuProvider {

	private static final long serialVersionUID = -5425600805782008997L;

	private final Saveable saver;

	/**
	 * Construct {@link PanelViewNode} that has a saver.
	 * 
	 * @param provider
	 *            {@link PanelViewProvider} that provides the {@link PanelView}
	 *            to be displayed when this node is clicked.
	 * @param saver
	 *            {@link Saveable} that saves the contents of
	 *            {@link PanelViewProvider} when required
	 */
	public SaveablePanelViewNode(PanelViewProvider provider, Saveable saver) {
		super(provider);
		this.saver = saver;
	}

	@Override
	public void saveToCSV(String directory) throws IOException {
		saver.saveToCSV(directory);
	}

	@Override
	public JPopupMenu getPopupMenu() {
		// TODO Auto-generated method stub
		return null;
	}

}