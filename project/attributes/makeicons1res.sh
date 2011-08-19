#!/bin/bash
#
# creates attribute icons in one resolution only
# target dir: ./drawable

require () {
    hash $1 2>&- || { echo >&2 "I require $1 but it's not installed.  Aborting."; exit 1; }
}

require optipng
require convert
require composite
require sed

# size of the image itself (inside border)
IMGSIZE=32
# size of the whole icon
ICONSIZE=48
# distance of border from edge of icon
BDIST=2
# thickness of border
BSTROKE=2
# size of the round edges
BROUND=8
# color of the border
FCOL=white
# background color of the icon
BCOL=black
# thickness of the strikethru bar
SSTROKE=5
# color of the strikethru bar
SCOL=\#c00000

#calculated values
BNDIST=$(( ${ICONSIZE} - ${BDIST} ))
res=48

# create output directory if missing
[ -d drawable ] || mkdir drawable

# create border
convert -size ${ICONSIZE}x${ICONSIZE} xc:none -fill ${BCOL} -strokewidth 1 \
    -draw "roundrectangle ${BDIST},${BDIST} ${BNDIST},${BNDIST} ${BROUND},${BROUND}" \
    -strokewidth ${BSTROKE} -stroke ${FCOL} \
    -draw "roundrectangle ${BDIST},${BDIST} ${BNDIST},${BNDIST} ${BROUND},${BROUND}" \
    border.png

# create strike-thru bar as overlay for _no images
convert -size ${ICONSIZE}x${ICONSIZE} xc:none -stroke "${SCOL}" -strokewidth ${SSTROKE} \
    -draw "line 0,0 ${ICONSIZE},${ICONSIZE}" border.png -compose DstIn -composite -depth 8 drawable/strikethru.png
optipng -quiet drawable/strikethru.png

if [ $# -gt 0 ]; then
    svgs="$@"
else
    svgs="svgs/*.svg"
fi
for s in $svgs; do
    n=drawable/attribute_`basename "$s" | sed "s/\.svg//"`

    # don't draw icons if svg is older than icon
    [ -f "${n}.png" ] && [ "$s" -ot "${n}.png" ] && continue

    echo "drawing $n"

    # draw icons
    convert -density 200 -background none "$s" -resize ${IMGSIZE}x${IMGSIZE} tmp.png
    composite -gravity center tmp.png border.png -depth 8 "${n}.png"
    optipng -quiet "${n}.png"
done


rm tmp.png border.png
