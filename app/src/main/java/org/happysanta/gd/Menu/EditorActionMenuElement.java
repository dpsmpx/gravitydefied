package org.happysanta.gd.Menu;

import android.view.View;

public class EditorActionMenuElement extends ClickableMenuElement {

    private final Runnable action;

    public EditorActionMenuElement(String text, Runnable action) {
        super(text);
        this.action = action;
    }

    @Override
    public void performAction(int k) {
        if ((k == MenuScreen.KEY_FIRE || k == MenuScreen.KEY_RIGHT) && action != null) {
            action.run();
        }
    }
}

class EditorViewMenuElement implements MenuElement {

    private final View view;

    EditorViewMenuElement(View view) {
        this.view = view;
    }

    @Override
    public boolean isSelectable() {
        return false;
    }

    @Override
    public View getView() {
        return view;
    }

    @Override
    public void setText(String text) {
    }

    @Override
    public void performAction(int k) {
    }
}
