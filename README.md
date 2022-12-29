Sets display range from max min pixel in the whole stack

Rather than just one sample image

Runs through each slice 

Note each type of ImageProcessor (ShortProcessor etc)

findMinAndMax sets min starting default to zero not max

So it always returns min is zero even if that's not true in the image.