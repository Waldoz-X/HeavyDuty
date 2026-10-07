# Third-party notices

## Body diagram geometry

The muscle outlines drawn by `HdBodyDiagram`
(`core/core-ui/src/main/assets/body_paths.json`) are derived from
[**MuscleMap**](https://github.com/melihcolpan/MuscleMap) by Melih Colpan, used under the
**MIT License** and reproduced below. MuscleMap ships its path data as Swift source rather
than `.svg` files; the paths were converted to JSON (male figure, front + back views only)
via [openGym](https://gitlab.com/DuarteSantos8/opengym)'s own MIT-compatible conversion,
and nothing else about the artwork was changed.

```
MIT License

Copyright (c) 2026 Melih Colpan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Exercise catalog

The exercise names, instructions and media references in
`core/core-data/src/main/assets/exercises.json` come from the
[**exercises-dataset**](https://github.com/hasaneyldrm/exercises-dataset) by hasaneyldrm. The
exercise images and animations, loaded from that repository at runtime, are credited by the
dataset as **© Gym visual — https://gymvisual.com/**. The bundled copy is trimmed to the
fields and languages the app uses (see `tools/trim_exercises.py`); the content itself is
unchanged.
