package android.widget;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import static com.applang.Util.*;

public class ImageView extends View
{
	protected static final String TAG = ImageView.class.getSimpleName();
	
	public ImageView(Context context) {
		super(context, null);
	}

	public ImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void create(Object... params) {
		Picture picture = new Picture();
		setComponent(picture);
	}
	
	public void setImage(Image image) {
		Picture picture = taggedComponent();
		if (picture != null)
			picture.setImage(image);
	}
	
    @Override
	public void applyAttributes() {
		if (attributeSet != null) {
	    	if (attributeSet.hasAttribute("android:src")) {
	    		Drawable drawable = attributeSet.getAttributeResourceItem("android:src");
				setImage(drawable.getImage());
			}
		}
		super.applyAttributes();
	}

}

class Picture extends JComponent implements MouseListener, FocusListener
{
    private Image image = null;

    public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public Picture() {
        setFocusable(true);
        addMouseListener(this);
        addFocusListener(this);
    }

    public void mouseClicked(MouseEvent e) {
        requestFocusInWindow();
    }

    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }

    public void focusGained(FocusEvent e) {
        this.repaint();
    }

    public void focusLost(FocusEvent e) {
        this.repaint();
    }

    protected void paintComponent(Graphics graphics) {
        Graphics g = graphics.create();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image == null ? 125 : image.getWidth(this),
                         image == null ? 125 : image.getHeight(this));
        if (image != null) {
            g.drawImage(image, 0, 0, this);
        }
        if (isFocusOwner()) {
            g.setColor(Color.RED);
        } 
        else {
            g.setColor(Color.BLACK);
        }
        g.drawRect(0, 0, image == null ? 125 : image.getWidth(this),
                         image == null ? 125 : image.getHeight(this));
        g.dispose();
    }
}
