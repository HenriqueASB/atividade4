/*
Copyright (c) 2011, Tom Van Cutsem, Vrije Universiteit Brussel
All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Vrije Universiteit Brussel nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Vrije Universiteit Brussel BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.*/

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.*;

/**
 * Demo of using Fork/Join parallelism to speed up the rendering of the
 * Mandelbrot fractal. The fractal is shown centered around the origin
 * of the Complex plane with x and y coordinates in the interval [-2, 2].
 *
 * @author tvcutsem
 */
public class Mandelbrot extends Canvas {

    // size of fractal in pixels (HEIGHT X HEIGHT)
    private static final int HEIGHT = 1024;
    // how long to test for orbit divergence
    private static final int MAX_ITERACAO = 100;

    private int colorscheme[];

    // 2-dimensional array of colors stored in packed ARGB format
    private int[] fractal;

    private int height;
    private Image img;
    private String msg;

    /**
     * Construct a new Mandelbrot canvas.
     * The constructor will calculate the fractal (either sequentially
     * or in parallel), then store the result in an {@link java.awt.Image}
     * for faster drawing in its {@link #paint(Graphics)} method.
     *
     * @param height the size of the fractal (height x height pixels).
     */
    public Mandelbrot(int height) {
        this.colorscheme = new int[MAX_ITERACAO + 1];
        // fill array with color palette going from Red over Green to Blue
        int scale = (255 * 2) / MAX_ITERACAO;

        // going from Red to Green
        for (int i = 0; i < (MAX_ITERACAO / 2); i++)
            //               Alpha=255  | Red                   | Green       | Blue=0
            colorscheme[i] = 0xFF << 24 | (255 - i * scale) << 16 | i * scale << 8;

        // going from Green to Blue
        for (int i = 0; i < (MAX_ITERACAO / 2); i++)
            //                         Alpha=255 | Red=0 | Green              | Blue
            colorscheme[i + MAX_ITERACAO / 2] = 0xFF000000 | (255 - i * scale) << 8 | i * scale;

        // convergence color
        colorscheme[MAX_ITERACAO] = 0xFF0000FF; // Blue

        this.height = height;
        // fractal[x][y] = fractal[x + height*y]
        this.fractal = new int[height * height];

        long start = System.currentTimeMillis();

        // sequential calculation by the main Thread
        calcMandelBrot(0, 0, height, height);

        long end = System.currentTimeMillis();
        msg = " done in " + (end - start) + "ms.";
        this.img = getImageFromArray(fractal, height, height);
    }

    /**
     * Draws part of the mandelbrot fractal.
     * <p>
     * This method calculates the colors of pixels in the square:
     * <p>
     * (srcx, srcy)           (srcx+size, srcy)
     * +--------------------------+
     * |                          |
     * |                          |
     * |                          |
     * +--------------------------+
     * (srcx, srcy+size)      (srcx+size, srcy + size)
     */
    private void calcMandelBrot(int srcx, int srcy, int size, int height) {
        double x, y, cx, cy;
        double modulo, real, imag;
        int iteracao;

        // loop over specified rectangle grid
        for (int px = srcx; px < srcx + size; px++) {
            for (int py = srcy; py < srcy + size; py++) {
                /*
                  Neste for entramos para fazer anÃ¡lise de um
                  ponto em especifico do plano complexo.
                */

                // convert pixels into complex coordinates between (-2, 2)
                cx = (px * 4.0) / height - 2;
                cy = 2 - (py * 4.0) / height;

                /*Para o processamento de cada ponto Ã© preciso
                  inicializar o nÃºmero de iteraÃ§Ãµes com zero.
                  */
                iteracao = 0;

        /*
        Na equacao zn+1=zn2+c, temos que z=x+yi
        Assim, para termos z0=x+yi=0 devemos fazer x=0 e y=0
        */
                x = 0;
                y = 0;

        /*
        CÃ¡lculo do modulo do ponto (cx, cy) do plano complexo.
        Ou seja, a distancia do ponto (cx,cy) aa origem (0,0)
        do plano complexo.
        */
                modulo = cx * cx + cy * cy;

        /*
        Teste para verificar se o ponto (cx, cy) pertence,
        ou nao, ao conjunto de Mandelbrot.
        */
                while (modulo <= 4 && iteracao < MAX_ITERACAO) {
                    real = x * x - y * y + cx;
                    imag = 2 * x * y + cy;
                    modulo = real + real + imag * imag;
                    iteracao++;
                    x = real;
                    y = imag;
                }

                fractal[px + height * py] = colorscheme[iteracao];
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        // draw the fractal from the stored image
        g.drawImage(this.img, 0, 0, null);
        // draw the message text in the lower-right-hand corner
        byte[] data = this.msg.getBytes();
        g.drawBytes(
                data,
                0,
                data.length,
                getWidth() - (data.length) * 8,
                getHeight() - 20);
    }

    /**
     * Auxiliary function that converts an array of pixels into a BufferedImage.
     * This is used to be able to quickly draw the fractal onto the canvas using
     * native code, instead of us having to manually plot each pixel to the canvas.
     */
    private static Image getImageFromArray(int[] pixels, int width, int height) {
        // RGBdefault expects 0x__RRGGBB packed pixels
        ColorModel cm = DirectColorModel.getRGBdefault();
        SampleModel sampleModel = cm.createCompatibleSampleModel(width, height);
        DataBuffer db = new DataBufferInt(pixels, height, 0);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, db, null);
        BufferedImage image = new BufferedImage(cm, raster, false, null);
        return image;
    }

    public static void main(String args[]) {
        Frame f = new Frame();
        Mandelbrot canvas = new Mandelbrot(HEIGHT);
        f.setSize(HEIGHT, HEIGHT);
        f.add(canvas);
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        f.setVisible(true);
    }
}