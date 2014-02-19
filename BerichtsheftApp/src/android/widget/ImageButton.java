package android.widget;

import java.awt.Dimension;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView.Picture;

public class ImageButton extends ImageView
{
	public ImageButton(Context context) {
		super(context);
	}

	public ImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
    protected void create() {
    	JButton button = new JButton();
		setComponent(button);
	}
	
    @Override
	public Image getImage() {
    	JButton button = taggedComponent();
		if (button != null && button.getIcon() instanceof ImageIcon) {
			ImageIcon imageIcon = (ImageIcon) button.getIcon();
			return imageIcon.getImage();
		}
		else
			return null;
	}
	
    @Override
	public void setImage(Image image) {
    	ImageIcon imageIcon = new ImageIcon(image);
    	JButton button = taggedComponent();
		if (button != null) {
			button.setIcon(imageIcon);
			Dimension size = new Dimension(imageIcon.getIconWidth(), imageIcon.getIconHeight());
			button.setPreferredSize(size);
		}
	}

}
