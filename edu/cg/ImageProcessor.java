package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {

    //MARK: Fields
    public final Logger logger;
    public final BufferedImage workingImage;
    public final RGBWeights rgbWeights;
    public final int inWidth;
    public final int inHeight;
    public final int workingImageType;
    public final int outWidth;
    public final int outHeight;

    //MARK: Constructors
    public ImageProcessor(Logger logger, BufferedImage workingImage,
                          RGBWeights rgbWeights, int outWidth, int outHeight) {
        super(); //Initializing for each loops...

        this.logger = logger;
        this.workingImage = workingImage;
        this.rgbWeights = rgbWeights;
        inWidth = workingImage.getWidth();
        inHeight = workingImage.getHeight();
        workingImageType = workingImage.getType();
        this.outWidth = outWidth;
        this.outHeight = outHeight;
        setForEachInputParameters();
    }

    public ImageProcessor(Logger logger,
                          BufferedImage workingImage,
                          RGBWeights rgbWeights) {
        this(logger, workingImage, rgbWeights,
                workingImage.getWidth(), workingImage.getHeight());
    }

    //MARK: Change picture hue - example
    public BufferedImage changeHue() {
        logger.log("Prepareing for hue changing...");

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;
        int max = rgbWeights.maxWeight;

        BufferedImage ans = newEmptyInputSizedImage();

        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int red = r * c.getRed() / max;
            int green = g * c.getGreen() / max;
            int blue = b * c.getBlue() / max;
            Color color = new Color(red, green, blue);
            ans.setRGB(x, y, color.getRGB());
        });

        logger.log("Changing hue done!");

        return ans;
    }

    //MARK: Nearest neighbor - example
    public BufferedImage nearestNeighbor() {
        logger.log("applies nearest neighbor interpolation.");
        BufferedImage ans = newEmptyOutputSizedImage();

        pushForEachParameters();
        setForEachOutputParameters();

        forEach((y, x) -> {
            int imgX = (int) Math.round((x * inWidth) / ((float) outWidth));
            int imgY = (int) Math.round((y * inHeight) / ((float) outHeight));
            imgX = Math.min(imgX, inWidth - 1);
            imgY = Math.min(imgY, inHeight - 1);
            ans.setRGB(x, y, workingImage.getRGB(imgX, imgY));
        });

        popForEachParameters();

        return ans;
    }

    //    MARK: Unimplemented methods
    public BufferedImage greyscale() {
        logger.log("Prepareing for greyscale changing...");

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;

        BufferedImage ans = newEmptyInputSizedImage();

        forEach((y, x) -> {
            Color c = new Color(workingImage.getRGB(x, y));
            int red = r * c.getRed();
            int green = g * c.getGreen();
            int blue = b * c.getBlue();
            int grey = (red + green + blue) / rgbWeights.weightsSum;
            Color color = new Color(grey, grey, grey);
            ans.setRGB(x, y, color.getRGB());
        });

        logger.log("Changing greyscale done!");

        return ans;
    }

    public BufferedImage gradientMagnitude() {
        if (inHeight < 2 | inWidth < 2) {
            throw new RuntimeException("Image too small..");
        }

        logger.log("Calculating gradient magnitude...");

        BufferedImage greyImg = greyscale(); // greyscale the original image
        BufferedImage ans = newEmptyInputSizedImage(); // the new image to be output

        forEach((y, x) -> {
            Color c = new Color(greyImg.getRGB(x, y));
            Color prevHorizontalColor;
            Color prevVerticalColor;
            int dx, dy;
            // edge pixel
            if (x == 0 && y == 0) {
                ans.setRGB(x, y, c.getRGB());
            } else {
                // calculates horizontal derivative
                if (x != 0) {
                    prevHorizontalColor = new Color(greyImg.getRGB(x - 1, y));
                    dx = c.getRed() - prevHorizontalColor.getRed();
                } else {
                    dx = 0;
                }
                // calculates vertical derivative
                if (y != 0) {
                    prevVerticalColor = new Color(greyImg.getRGB(x, y - 1));
                    dy = (c.getRed() - prevVerticalColor.getRed());
                } else {
                    dy = 0;
                }
                // both gradients calculation
                int res = (int) Math.sqrt((Math.pow(dx, 2) + Math.pow(dy, 2)) / 2);
                int magnitude = Math.abs(res);
                // new grey pixel
                Color color = new Color(magnitude, magnitude, magnitude);

                ans.setRGB(x, y, color.getRGB());
            }
        });

        logger.log("Calculating gradient magnitude done!");

        return ans;
    }


    public BufferedImage bilinear() {

        logger.log("applies bilinear interpolation");

        pushForEachParameters();
        setForEachOutputParameters();
        BufferedImage ans = newEmptyOutputSizedImage();

        forEach((y, x) -> {
            // a pixel's coordinates in the resized image
            float imgX = x * inWidth / (float) outWidth;
            float imgY = y * inHeight / (float) outHeight;
            // creating a combination of 4 points
            int x1 = (int) Math.floor(imgX);
            int y1 = (int) Math.floor(imgY);
            int x2 = (int) Math.ceil(imgX);
            int y2 = (int) Math.ceil(imgY);
            // the new rgb value in y=y1 between x1,x2
            int c11 = this.workingImage.getRGB(x1, y1);
            int c21 = this.workingImage.getRGB(x2, y1);
            int dx1 = distanceWeight(x2 - imgX, c11, c21);
            // the new rgb value in y=y2 between x1,x2
            int c12 = this.workingImage.getRGB(x1, y2);
            int c22 = this.workingImage.getRGB(x2, y2);
            int dx2 = distanceWeight(x2 - imgX, c12, c22);
            // the final rgb value for the chosen point to represent a pixel
            int dy = distanceWeight(y2 - imgY, dx1, dx2);

            ans.setRGB(x, y, dy);
        });

        popForEachParameters();
        return ans;
    }

    private int distanceWeight(float d, int c1, int c2) {
        // gets rgb for one edge pixel
        int r1 = (c1 >> 16) & 255;
        int g1 = (c1 >> 8) & 255;
        int b1 = c1 & 255;
        // gets rgb for the other edge pixel
        int r2 = (c2 >> 16) & 255;
        int g2 = (c2 >> 8) & 255;
        int b2 = c2 & 255;
        // calculates weighted sum
        int new_r = (int) (d * r1 + (1 - d) * r2);
        int new_g = (int) (d * g1 + (1 - d) * g2);
        int new_b = (int) (d * b1 + (1 - d) * b2);
        // init rgb inside int the same as getRGB works
        int rgb = new_r << 8;
        rgb = (rgb | new_g) << 8;
        rgb |= new_b;
        // returns the rgb int value of the new pixel
        return rgb;
    }

    //MARK: Utilities
    public final void setForEachInputParameters() {
        setForEachParameters(inWidth, inHeight);
    }

    public final void setForEachOutputParameters() {
        setForEachParameters(outWidth, outHeight);
    }

    public final BufferedImage newEmptyInputSizedImage() {
        return newEmptyImage(inWidth, inHeight);
    }

    public final BufferedImage newEmptyOutputSizedImage() {
        return newEmptyImage(outWidth, outHeight);
    }

    public final BufferedImage newEmptyImage(int width, int height) {
        return new BufferedImage(width, height, workingImageType);
    }

    public final BufferedImage duplicateWorkingImage() {
        BufferedImage output = newEmptyInputSizedImage();

        forEach((y, x) ->
                output.setRGB(x, y, workingImage.getRGB(x, y))
        );

        return output;
    }
}
