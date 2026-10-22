"""Pixel-authored ice crystals and their clipped translucent cross models.

Run with a Python environment containing Pillow. The small sprite is drawn at
native pixel density, not resampled from the large one. Empty pixels have no
geometry so they cannot write depth over neighboring translucent ice.
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/frostedheart'
PALETTE = {
    '.': (0, 0, 0, 0),
    '1': (190, 201, 209, 204),
    '2': (210, 219, 224, 204),
    '3': (229, 234, 238, 204),
    '4': (242, 245, 247, 204),
    '5': (252, 253, 254, 204),
}
# Large silhouette/facets derive from Mojang's 26.4-snapshot-5 ice_crystal.png.
# Its five blue shades are replaced by the cool-white palette above.
LARGE = [
    '................',
    '........5.......',
    '........5.......',
    '.......54.......',
    '.......43.......',
    '.....5.324......',
    '.....52213......',
    '....435112.4....',
    '....32411253....',
    '..5.21314242....',
    '..54112252312...',
    '.534112432213.5.',
    '.42311232413234.',
    '.312132313112232',
    '.212312212122122',
    '.212112112121112',
]
# Independently drawn three-shard juvenile cluster, seven native pixels high.
SMALL = ['................'] * 9 + [
    '.......5........',
    '.......54.......',
    '....5..53.......',
    '....54.42..5....',
    '...532.325.54...',
    '...4232324342...',
    '...3121212321...',
]


def rectangles(rows):
    """Merge identical occupied row spans, splitting at the crossed-plane axis."""
    active = {}
    result = []
    for v in range(17):
        spans = []
        if v < 16:
            for lo, hi in ((0, 8), (8, 16)):
                u = lo
                while u < hi:
                    if rows[v][u] == '.':
                        u += 1
                        continue
                    start = u
                    while u < hi and rows[v][u] != '.':
                        u += 1
                    spans.append((start, u))
        for span in list(active):
            if span not in spans:
                result.append((*span, active.pop(span), v))
        for span in spans:
            active.setdefault(span, v)
    return result


def model(rows, texture):
    # Same unscaled vanilla cross coordinates for BOTH models: one native pixel
    # is one model unit tall, while the diagonal sheet spans 14.4 model units.
    elements = {}
    for u0, u1, v0, v1 in rectangles(rows):
        for face in ('north', 'south', 'west', 'east'):
            reverse = face in ('north', 'east')
            lo = 0.8 + 0.9 * (16 - u1 if reverse else u0)
            hi = 0.8 + 0.9 * (16 - u0 if reverse else u1)
            axis = 0 if face in ('north', 'south') else 2
            low, high = [8., 16. - v1, 8.], [8., 16. - v0, 8.]
            low[axis], high[axis] = round(lo, 6), round(hi, 6)
            key = (*low, *high)
            element = elements.setdefault(key, {
                'from': low, 'to': high,
                'rotation': {'origin': [8, 0, 8], 'axis': 'y', 'angle': 45, 'rescale': True},
                'shade': False, 'faces': {},
            })
            element['faces'][face] = {'uv': [u0, v0, u1, v1], 'texture': '#cross'}
    return {'parent': 'minecraft:block/cross', 'render_type': 'minecraft:translucent',
            'textures': {'cross': f'frostedheart:block/{texture}'},
            'elements': list(elements.values())}


def validate(rows, data):
    # Independently sample each original face at every texel center. All visible
    # texels must occur exactly once at the original UV; empty texels occur zero times.
    for face in ('north', 'south', 'west', 'east'):
        axis = 0 if face in ('north', 'south') else 2
        reverse = face in ('north', 'east')
        for v in range(16):
            for u in range(16):
                along = 0.8 + 0.9 * (15.5 - u if reverse else u + .5)
                y = 15.5 - v
                hits = []
                for e in data['elements']:
                    if face not in e['faces'] or not (e['from'][axis] < along < e['to'][axis]
                            and e['from'][1] < y < e['to'][1]):
                        continue
                    uv = e['faces'][face]['uv']
                    fraction = (along - e['from'][axis]) / (e['to'][axis] - e['from'][axis])
                    if reverse:
                        fraction = 1 - fraction
                    mapped_u = uv[0] + fraction * (uv[2] - uv[0])
                    mapped_v = uv[3] - (y - e['from'][1]) / (e['to'][1] - e['from'][1]) * (uv[3] - uv[1])
                    hits.append((mapped_u, mapped_v))
                assert len(hits) == (rows[v][u] != '.'), (face, u, v, hits)
                if hits:
                    assert abs(hits[0][0] - u - .5) < 1e-5 and abs(hits[0][1] - v - .5) < 1e-5
    for e in data['elements']:
        for x in (e['from'][0], e['to'][0]):
            for z in (e['from'][2], e['to'][2]):
                assert 0 <= 8 + (x - 8) - (z - 8) <= 16
                assert 0 <= 8 + (x - 8) + (z - 8) <= 16


def write_model(path, data):
    # Keep numeric arrays and each face readable on one line.
    lines = ['{', *(f'  {json.dumps(key)}: {json.dumps(value)},'
                   for key, value in data.items() if key != 'elements'), '  "elements": [']
    for i, e in enumerate(data['elements']):
        lines += ['    {', ',\n'.join(f'      {json.dumps(key)}: {json.dumps(value)}'
                                    for key, value in e.items()),
                  '    }' + (',' if i < len(data['elements']) - 1 else '')]
    path.write_text('\n'.join(lines + ['  ]', '}', '']), encoding='utf-8')


if __name__ == '__main__':
    for name, texture, rows in [('large_ice_crystal', 'ice_crystal', LARGE),
                                ('ice_crystal', 'small_ice_crystal', SMALL)]:
        assert len(rows) == 16 and all(len(row) == 16 for row in rows)
        image = Image.new('RGBA', (16, 16))
        image.putdata([PALETTE[p] for row in rows for p in row])
        image.save(ASSETS / f'textures/block/{texture}.png')
        data = model(rows, texture)
        validate(rows, data)
        write_model(ASSETS / f'models/block/{name}.json', data)
        count = sum(p != '.' for row in rows for p in row)
        print(f'{name}: {count} pixels, alpha=204, {len(data["elements"])} elements, '
              f'{sum(len(e["faces"]) for e in data["elements"])} faces; 1024 texel/face samples valid')
