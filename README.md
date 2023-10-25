AutoLevel Stack ImageJ plugin

MIT license, but with no advertising clause

Public domain, copyright Prof Phil Threlfall-Holmes, TH Collaborative Innovation, 2022,2023


Sets display range from max min pixel in the whole stack

Rather than just one sample image

Runs through each slice 

Note each type of ImageProcessor (ShortProcessor etc)

Built-in method findMinAndMax sets min starting default to zero not max

So it always returns min is zero even if that's not true in the image.

In this plugin, min is set default before loop as bit-level max, (c.f. in-built method set as 0)

so the true min pixel greyscale in the stack is found: in-built method will always find 0.


Based on Johannes Schindelin's plugin tutorial template for processing each pixel of either
GRAY8, GRAY16, GRAY32 or COLOR_RGB images.
