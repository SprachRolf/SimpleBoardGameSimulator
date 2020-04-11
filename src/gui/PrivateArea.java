package gui;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;

public class PrivateArea {
    public int width = 0;
    public int height = 0;
    public Shape shape = null;

    public PrivateArea(){
    }

    public void setArea(double posX, double posY, double width,  double height, int translateX, int translateY, double rotation, double zooming) {
        AffineTransform tx = new AffineTransform();
        tx.translate(-translateX, -translateY);
        tx.scale(1/zooming, 1/zooming);
        tx.rotate(-rotation);
        Shape privateArea = new Arc2D.Double(posX, posY, width, height, 0, 180, Arc2D.OPEN);


        this.shape = tx.createTransformedShape(privateArea);
    }

    public void draw(Graphics g)
    {
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // set background color
        graphics.setPaint(Color.LIGHT_GRAY);
        // set border color
        graphics.setColor(Color.GRAY);
        graphics.setStroke(new BasicStroke(2));
        graphics.fill(this.shape);
    }
}
