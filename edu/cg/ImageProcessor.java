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

        int r = rgbWeights.redWeight;
        int g = rgbWeights.greenWeight;
        int b = rgbWeights.blueWeight;

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
                int magnitude = Math.abs(255 - res);
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
        BufferedImage ans = newEmptyOutputSizedImage();

        pushForEachParameters();
        setForEachOutputParameters();

        double prev_imgX = 0;
        double prev_imgY = 0;
        forEach((y, x) -> {
            double imgX = x * inWidth / (float) outWidth;
            double imgY = y * inHeight / (float) outHeight;

            int x1 = (int) Math.floor(imgX);
            int x2 = (int) Math.ceil(imgX);
            int y1 = (int) Math.floor(imgY);
            int y2 = (int) Math.ceil(imgY);

            int c1 = workingImage.getRGB(x1, y1);
            int c2 = workingImage.getRGB(x2, y1);
            int c3 = workingImage.getRGB(x1, y2);
            int c4 = workingImage.getRGB(x2, y2);

            if (x1 != x2) {

                double t = (imgX - x1) / (x2 - x1);
                double c12 = t * c2 + (1 - t) * c1;

                if (y1 != y2) {
                    double c34 = t * c4 + (1 - t) * c3;
                    double s = (imgY - y1) / (y2 - y1);
                    double c = (1 - s) * c12 + s * c34;

                    ans.setRGB(x, y, (int) c);

                } else ans.setRGB(x, y, (int) c12);
                // if x1 = x2
            } else if (y1 != y2) {
                double s = (imgY - y1) / (y2 - y1);
                double c = (1 - s) * c1 + s * c3;

                ans.setRGB(x, y, (int) c);
            } else ans.setRGB(x, y, workingImage.getRGB((int) imgX, (int) imgY));
        });

        popForEachParameters();

        return ans;
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
