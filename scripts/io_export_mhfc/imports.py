from contextlib import ExitStack
from math import radians, pi
from xml.etree import ElementTree
from zipfile import ZipFile
import bmesh
import bpy
import json
import os

from mathutils import Matrix, Vector, Quaternion, Euler

from .utils import Reporter


def import_tabula4(tblzip, jsonmodel, animations_only, scene):
    with ExitStack() as stack:
        bm = bmesh.new()
        stack.callback(bm.free)

        model_name = jsonmodel['modelName']
        author = jsonmodel['authorName']

        mesh = bpy.data.meshes.new(model_name)
        mesh.mcprops.artist = author
        uvmap = mesh.uv_textures.new('UVMap')
        bm.from_mesh(mesh)
        uvloop = bm.loops.layers.uv[uvmap.name]

        identifier_to_cube = set()

        tex_width = jsonmodel['textureWidth']
        tex_height = jsonmodel['textureHeight']
        texture_matrix = Matrix.Identity(3)
        texture_matrix[0][0] = 1 / tex_width
        texture_matrix[1][1] = -1 / tex_height
        texture_matrix[1][2] = 1
        print(texture_matrix)

        texture_name = 'texture.png'
        try:
            tblzip.extract(texture_name, path=bpy.app.tempdir)
            texture_file = os.path.join(bpy.app.tempdir, texture_name)
        except KeyError:
            Reporter.error(
                "Missing texture file", texname=texture_file)
        else:
            image = bpy.data.images.load(texture_file)
            image.pack()
            image.use_alpha = True
            os.remove(texture_file)

        def Position(vec):
            s = Matrix.Identity(4)
            s.row[0][3], s.row[1][3], s.row[2][3] = vec
            return s

        def Scale(vec):
            s = Matrix.Identity(4)
            s.row[0][0], s.row[1][1], s.row[2][2] = vec
            return s

        def Rotation(rot):
            return Euler(rot, 'XYZ').to_matrix().to_4x4()

        def emit_cube(cube, local_to_global):
            offset = Position(cube['offset'])  # Offset
            dim_x, dim_y, dim_z = cube['dimensions']
            dimensions = Scale((dim_x, dim_y, dim_z))
            matrix = local_to_global * offset * dimensions
            tx_transform = Matrix.Identity(3)
            tx_transform.col[2] =\
                cube['txOffset'][0], cube['txOffset'][1], 1
            tx_transform = texture_matrix * tx_transform
            print(tx_transform)

            v0 = bm.verts.new((matrix * Vector((0, 0, 0, 1)))[0:3])
            v1 = bm.verts.new((matrix * Vector((0, 0, 1, 1)))[0:3])
            v2 = bm.verts.new((matrix * Vector((0, 1, 0, 1)))[0:3])
            v3 = bm.verts.new((matrix * Vector((0, 1, 1, 1)))[0:3])
            v4 = bm.verts.new((matrix * Vector((1, 0, 0, 1)))[0:3])
            v5 = bm.verts.new((matrix * Vector((1, 0, 1, 1)))[0:3])
            v6 = bm.verts.new((matrix * Vector((1, 1, 0, 1)))[0:3])
            v7 = bm.verts.new((matrix * Vector((1, 1, 1, 1)))[0:3])
            bm.edges.new((v0, v1))
            bm.edges.new((v0, v2))
            bm.edges.new((v0, v4))
            bm.edges.new((v1, v3))
            bm.edges.new((v1, v5))
            bm.edges.new((v2, v3))
            bm.edges.new((v2, v6))
            bm.edges.new((v3, v7))
            bm.edges.new((v4, v5))
            bm.edges.new((v4, v6))
            bm.edges.new((v5, v7))
            bm.edges.new((v6, v7))
            f0 = bm.faces.new((v0, v1, v3, v2))
            f1 = bm.faces.new((v0, v4, v5, v1))
            f2 = bm.faces.new((v0, v2, v6, v4))
            f3 = bm.faces.new((v4, v6, v7, v5))
            f4 = bm.faces.new((v2, v3, v7, v6))
            f5 = bm.faces.new((v1, v5, v7, v3))
            f0.loops[0][uvloop].uv = (
                tx_transform * Vector((dim_z, dim_z, 1)))[0:2]
            f0.loops[1][uvloop].uv = (
                tx_transform * Vector((0, dim_z, 1)))[0:2]
            f0.loops[2][uvloop].uv = (
                tx_transform * Vector((0, dim_z + dim_y, 1)))[0:2]
            f0.loops[3][uvloop].uv = (
                tx_transform * Vector((dim_z, dim_z + dim_y, 1)))[0:2]
            f1.loops[0][uvloop].uv = (
                tx_transform * Vector((dim_z, dim_z, 1)))[0:2]
            f1.loops[1][uvloop].uv = (
                tx_transform * Vector((dim_z + dim_x, dim_z, 1)))[0:2]
            f1.loops[2][uvloop].uv = (
                tx_transform * Vector((dim_z + dim_x, 0, 1)))[0:2]
            f1.loops[3][uvloop].uv = (
                tx_transform * Vector((dim_z, 0, 1)))[0:2]
            f2.loops[0][uvloop].uv = (
                tx_transform * Vector((dim_z, dim_z, 1)))[0:2]
            f2.loops[1][uvloop].uv = (
                tx_transform * Vector((dim_z, dim_z + dim_y, 1)))[0:2]
            f2.loops[2][uvloop].uv = (
                tx_transform * Vector((dim_z + dim_x, dim_z + dim_y, 1)))[0:2]
            f2.loops[3][uvloop].uv = (
                tx_transform * Vector((dim_z + dim_x, dim_z, 1)))[0:2]
            f3.loops[0][uvloop].uv = (
                tx_transform * Vector((2 * dim_z + 2 * dim_x, dim_z, 1)))[0:2]
            f3.loops[1][uvloop].uv = (
                tx_transform * Vector((dim_z + 2 * dim_x, dim_z, 1)))[0:2]
            f3.loops[2][uvloop].uv = (
                tx_transform * Vector((dim_z + 2 * dim_x, dim_z + dim_y, 1)))[0:2]
            f3.loops[3][uvloop].uv = (
                tx_transform * Vector((2 * dim_z + 2 * dim_x, dim_z + dim_y, 1)))[0:2]
            f4.loops[0][uvloop].uv = (
                tx_transform * Vector((dim_z + 2 * dim_x, dim_z, 1)))[0:2]
            f4.loops[1][uvloop].uv = (
                tx_transform * Vector((dim_z + 2 * dim_x, 0, 1)))[0:2]
            f4.loops[2][uvloop].uv = (
                tx_transform * Vector((dim_z + dim_x, 0, 1)))[0:2]
            f4.loops[3][uvloop].uv = (
                tx_transform * Vector((dim_z + dim_x, dim_z, 1)))[0:2]
            f5.loops[0][uvloop].uv = (
                tx_transform * Vector((dim_z + dim_x, dim_z, 1)))[0:2]
            f5.loops[1][uvloop].uv = (
                tx_transform * Vector((dim_z + dim_x, dim_z + dim_y, 1)))[0:2]
            f5.loops[2][uvloop].uv = (
                tx_transform * Vector((dim_z + 2 * dim_x, dim_z + dim_y, 1)))[0:2]
            f5.loops[3][uvloop].uv = (
                tx_transform * Vector((dim_z + 2 * dim_x, dim_z, 1)))[0:2]
            bm.verts.index_update()
            bm.edges.index_update()
            bm.faces.index_update()

            bm.normal_update()
            return

        def make_cube(cube, to_global):
            identifier = cube['identifier']
            name = cube['name']

            position = Position(cube['position'])  # Rotation point
            scale = Scale(cube['scale'])
            rotation = Rotation(cube['rotation'])

            local_to_global = to_global * position * rotation * scale

            if identifier in identifier_to_cube:
                Reporter.warning(
                    "Identifier reused in the model: {id}", id=identifier)
            identifier_to_cube.add(identifier)

            emit_cube(cube, local_to_global)

            for child in cube['children']:
                make_cube(child, local_to_global)

        def make_group(group, to_global):
            for cube in group['cubes']:
                make_cube(cube, to_global)
            for child in group['cubeGroups']:
                make_cube(child, to_global)

        model_transform = Matrix.Scale(1 / 16, 4)
        model_transform[1][2] = model_transform[2][2]
        model_transform[2][1] = -model_transform[1][1]
        model_transform[1][1] = model_transform[2][2] = 0
        model_transform[2][3] = 51 / 32
        model_transform *= Scale(jsonmodel['scale'])

        for group in jsonmodel['cubeGroups']:
            make_group(group, model_transform)
        for cube in jsonmodel['cubes']:
            make_cube(cube, model_transform)

        bm.to_mesh(mesh)
        object = bpy.data.objects.new(model_name, mesh)
        scene.objects.link(object)
        scene.update()

    def make_animation(*args):
        pass  # TODO: import animations aswell?

    for animation in jsonmodel['anims']:
        make_animation(animation, model_transform)

import_fns = {
    4: import_tabula4,
    3: import_tabula4,
    2: import_tabula4
}


def import_tabula(filepath, scene, animations_only):
    with ZipFile(filepath, 'r') as tabula:
        modelstr = tabula.read('model.json').decode()
        model = json.loads(modelstr)
        # Successfully loaded the model json, now convert it
        version = model['projVersion']
        try:
            import_fns[version](tabula, model, animations_only, scene)
        except (KeyError, NotImplementedError) as e:
            Reporter.fatal(
                "tabula version {v} is not (yet) supported".format(v=version))


class ElementFactory(object):

    def __init__(self, tcnfile, **wargs):
        self.registry = wargs
        self.tcn = tcnfile

    def __call__(self, xml):
        try:
            typ = self.registry[xml.tag]
        except KeyError:
            Reporter.fatal("Unknown Element: {name}", name=xml.tag)
        else:
            try:
                return typ(xml, self.tcn)
            except Exception as ex:
                Reporter.fatal(
                    "Failed to initialize the element {tag}", tag=xml.tag, cause=ex)


class Element(object):
    """Each xml entry gets transformed to an Element"""

    def __init__(self, *args, **wargs):
        pass

    def apply(self):
        """Applies the element and returns a suitable value"""
        raise NotImplementedError()

    def append(self, subelement):
        """Appends the subelement to this."""
        raise NotImplementedError()


class Techne(Element):
    """The abstract root element"""

    def _init__(self, xml, tcn):
        self.version = xml.attrib['Version']
        self.author = None
        self.models = None
        self.name = None

    def append(self, subelement, factory):
        tag = subelement.tag
        if tag == 'Author':
            author = subelement.text
            self.author = author
        elif tag == 'Models':
            self.models = []
            return self
        elif tag == 'Model':
            model = factory(subelement)
            self.models.append(model)
            return model
        elif tag == 'Name':
            name = subelement.text
            self.name = name
        elif tag not in ('DateCreated', 'Description', 'PreviewImage', 'ProjectName', 'ProjectType'):
            Reporter.warning("Unknown tag ({tag}) in <Techne>", tag=tag)

    def apply(self):
        if self.models is None:
            Reporter.fatal("Invalid model.xml: No 'Models' tag.")
        if self.author is None:
            Reporter.warning("No author found, defaulting to WorldSEnder")
            self.author = "WorldSEnder"
        if len(self.models) == 1 and self.name:
            self.models[0].name = self.name
        objects = []
        for model in self.models:
            obj = model.apply()
            if obj is None:
                continue
            obj.data.mcprops.artist = self.author
            objects.append(obj)
        return objects


class Model(Element):
    """A model in the techne file"""

    def __init__(self, xml, tcn):
        tex = xml.attrib['texture']
        extr_file = os.path.join(bpy.app.tempdir, tex)
        try:
            tcn.extract(tex, path=bpy.app.tempdir)
        except KeyError:
            Reporter.warning(
                "Missing texture {texname} (was one supplied?)", texname=tex)
            self.img = None
        else:
            self.img = bpy.data.images.load(extr_file)
            self.img.pack()
            self.img.use_alpha = True
            os.remove(extr_file)
        self.shapes = None
        self.obj_scale = None
        self.uv_scale = None
        self.name = None

    def append(self, subelement, factory):
        tag = subelement.tag
        if tag == 'Name':
            self.name = subelement.text
        elif tag == 'GlScale':
            mat = Matrix.Identity(3)
            mat[0][0], mat[1][1], mat[2][2] = map(
                float, subelement.text.split(','))
            self.obj_scale = mat
        elif tag == 'TextureSize':
            mat = Matrix.Identity(2)
            mat[0][0], mat[1][1] = map(
                lambda x: 1 / int(x), subelement.text.split(','))
            self.uv_scale = mat
        elif tag == 'Geometry':
            self.shapes = []
            return self
        elif tag == 'Folder':
            return self
        elif tag in ('Shape', 'Piece'):
            piece = factory(subelement)
            self.shapes.append(piece)
            return piece
        elif tag not in ('BaseClass',):
            Reporter.warning("Unknown tag ({tag}) in <Model>", tag=tag)

    def apply(self):
        if self.name is None:
            Reporter.fatal("Missing nametag in model")
        if self.shapes is None:
            Reporter.fatal("Missing shapes in model {name}", name=self.name)
        if self.obj_scale is None:
            Reporter.warning(
                "Missing GlScale in model {name}, assuming Identity", name=self.name)
        if self.uv_scale is None:
            Reporter.warning(
                "Missing texture-size in model {name}. Computing from texture anyway", name=self.name)
        elif self.img is not None and (self.uv_scale[0][0] != 1 / self.img.size[0] or self.uv_scale[1][1] != 1 / self.img.size[1]):
            Reporter.warning("In Model {name}: Texture is ({x}, {y}) but .tcn file said ({x_xml}, {y_xml})",
                             name=self.name, x=self.img.size[0], y=self.img.size[1], x_xml=self.uv_scale[0], y_xml=self.uv_scale[1])
        # make mesh
        mesh = bpy.data.meshes.new(self.name)
        bm = bmesh.new()
        uv = bm.loops.layers.uv.new('UVLayer')
        tex = bm.faces.layers.tex.new('UVLayer')
        mc_loop = bm.faces.layers.int.new('MCRenderGroupIndex')
        for shape_nbr, shape in enumerate(self.shapes):
            # points is a list of lists of points (representing a list of
            # faces)
            points, idx_uvs = shape.apply(True)
            bpoints = []
            for p in points:
                co = self.obj_scale * p[0]
                norm = self.obj_scale * p[1]
                co.y = 24 - co.y
                co = Matrix(
                    ((-1 / 16, 0, 0), (0, 0, -1 / 16), (0, 1 / 16, 0))) * co
                norm = Matrix(((-1, 0, 0), (0, 0, -1), (0, 1, 0))) * norm
                bp = bm.verts.new(co)
                bp.normal = norm
                bpoints.append(bp)
            for face in idx_uvs:
                bface = bm.faces.new([bpoints[l[0]] for l in face])
                for loop, (_, uvco) in zip(bface.loops, face):
                    uvco = self.uv_scale * uvco
                    uvco[1] = 1 - uvco[1]
                    loop[uv].uv = uvco
                bface[tex].image = self.img
                if mc_loop is not None:
                    bface[mc_loop] = shape_nbr + 1
        bm.verts.index_update()
        bm.faces.index_update()
        bm.to_mesh(mesh)
        # make obj
        obj = bpy.data.objects.new(self.name, mesh)
        props = mesh.mcprops
        props.name = self.name
        props.uv_layer = uv.name
        if self.img is not None:
            props.default_group.image = self.img.name
        for shape in self.shapes:
            group = props.render_groups.add()
            group.name = shape.name
            if self.img is not None:
                group.image = self.img.name
        return obj


class Piece(Element):

    def __init__(self, xml, tcn):
        assert(xml.attrib['type'] == "9ec93754-b48d-4c70-ae4e-84d81ae55396")
        self.position = None
        self.rotation = None
        self.sub_shapes = None
        self.name = xml.attrib['Name']

    def append(self, subelement, factory):
        tag = subelement.tag
        if tag == 'Position':
            self.position = Vector(map(float, subelement.text.split(',')))
        elif tag == 'Rotation':
            rot_x, rot_y, rot_z = map(float, subelement.text.split(','))
            self.rotation = rotation_matrix_from_euler(rot_x, rot_y, rot_z)
        elif tag == 'Folder':
            return self
        elif tag == 'Shape':
            if self.sub_shapes is None:
                self.sub_shapes = []
            shape = factory(subelement)
            self.sub_shapes.append(shape)
            return shape

    def apply(self, apply_position):
        if self.position is None:
            Reporter.fatal(
                "Missing Position-property in Piece {name}", name=self.name)
        if self.rotation is None:
            Reporter.fatal(
                "Missing Rotation-property in Piece {name}", name=self.name)
        if self.sub_shapes is None:
            Reporter.fatal("Missing shapes in Piece {name}", name=self.name)
        ps, idxuvs = [], []
        rot = self.rotation
        pos = self.position
        for shape in self.sub_shapes:
            shape_ps, shape_idxuvs = shape.apply(False)
            idx_off = len(ps)
            ps += [(rot * p + pos, n) for p, n in shape_ps]
            idxuvs += [(i + idx_off, uv) for i, uv in shape_idxuvs]
        return ps, idxuvs


class Shape(Element):

    def __init__(self, xml, tcn):
        self.name = xml.attrib['name']
        self.is_mirrored = None
        self.offset = None
        self.position = None
        self.rotation = None
        self.size = None
        self.texture_offset = None

    def __new__(cls, xml, tcn):
        class Debug(Shape):

            def __init__(self, xml, tcn):
                super().__init__(xml, tcn)

            def template_shape(self, size):
                return [], []
        if cls is not Shape:
            return super(Shape, cls).__new__(cls)
        typ = xml.attrib['type']
        if typ == "d9e621f7-957f-4b77-b1ae-20dcd0da7751":
            return Cube(xml, tcn)  # cube
        elif typ == "0900de04-664f-4789-8562-07ffe1043e90":
            Reporter.warning("Instantiating placeholder instead of Cone")
            return Debug(xml, tcn)  # cone
        elif typ == "b94b0064-e61c-4517-8f99-adb273f1b33e":
            Reporter.warning("Instantiating placeholder instead of Cylinder")
            return Debug(xml, tcn)  # cylinder
        elif typ == "e1957603-6c07-4a1e-9047-bb1f45e57cea":
            Reporter.warning("Instantiating placeholder instead of Sphere")
            return Debug(xml, tcn)  # sphere
        elif typ == "de81aa14-bd60-4228-8d8d-5238bcd3caaa":
            Reporter.warning("Instantiating placeholder instead of TMT Cube")
            return Debug(xml, tcn)  # tmt cube

    def append(self, subelement, factory):
        tag = subelement.tag
        if tag in ('Animation', 'AnimationAngles', 'AnimationType'):
            return self
        elif tag == 'IsMirrored':
            val = subelement.text
            self.is_mirrored = val == "True"
        elif tag == 'Offset':
            self.offset = Vector(map(float, subelement.text.split(',')))
        elif tag == 'Position':
            self.position = Vector(map(float, subelement.text.split(',')))
        elif tag == 'Rotation':
            rot_x, rot_y, rot_z = map(float, subelement.text.split(','))
            self.rotation = rotation_matrix_from_euler(rot_x, rot_y, rot_z)
        elif tag == 'Size':
            self.size = Vector(map(int, subelement.text.split(',')))
        elif tag == 'TextureOffset':
            self.texture_offset = Vector(map(int, subelement.text.split(',')))

    def apply(self, apply_position):
        """
        Should return a list of points (coord, normal) and a list
        of indices with mixed in uv-coords like this:
        [(co0, norm0), ((), ()), ...], [((idx0, uv0), (), ...), ...]
        If apply_offset the offset should be already applied
        """
        if self.is_mirrored is None:
            Reporter.fatal(
                "Missing IsMirrored-property in Shape {name}", name=self.name)
        if self.offset is None:
            Reporter.fatal(
                "Missing Offset-property in Shape {name}", name=self.name)
        if self.position is None:
            Reporter.fatal(
                "Missing Position-property in Shape {name}", name=self.name)
        if self.rotation is None:
            Reporter.fatal(
                "Missing Rotation-property in Shape {name}", name=self.name)
        if self.size is None:
            Reporter.fatal(
                "Missing Size-property in Shape {name}", name=self.name)
        if self.texture_offset is None:
            Reporter.fatal(
                "Missing TextureOffset-property in Shape {name}", name=self.name)
        points, idx_uvs = self.template_shape(self.size)
        off = self.offset
        rot = self.rotation
        pos = self.position
        points = [(p + off, n) for p, n in points]
        if apply_position:
            points = [(rot * p + pos, rot * n) for p, n in points]
        tex_off = self.texture_offset
        idx_uvs = [[(idx, uv + tex_off) for idx, uv in face]
                   for face in idx_uvs]
        if self.is_mirrored:
            m_mat = Matrix.Scale(-1, 3, (1, 0, 0))
            idx_offset = len(points)
            points += [(m_mat * p, m_mat * n) for p, n in points]
            idx_uvs += [[(idx + idx_offset, uv)
                         for idx, uv in face[::-1]] for face in idx_uvs]
        return points, idx_uvs

    def template_shape(self):
        """
        Templates the shape at 0, 0, 0.
        """
        raise NotImplementedError()


class Cube(Shape):

    def __init__(self, xml, tcn):
        super().__init__(xml, tcn)

    def template_shape(self, size):
        x, y, z = size
        return ([(Vector((x, y, 0)), Vector((0.333333, 0.666667, -0.666667))),
                 (Vector((0, y, 0)), Vector((-0.816497, 0.408248, -0.408248))),
                 (Vector((0, y, z)), Vector((-0.333333, 0.666667, 0.666667))),
                 (Vector((x, y, z)), Vector((0.816497, 0.408248, 0.408248))),
                 (Vector((x, 0, 0)), Vector((0.666667, -0.666667, -0.333333))),
                 (Vector((0, 0, 0)), Vector(
                     (-0.408248, -0.408248, -0.816497))),
                 (Vector((0, 0, z)), Vector((-0.666667, -0.666667, 0.333333))),
                 (Vector((x, 0, z)), Vector((0.408248, -0.408248, 0.816497)))],
                [((5, Vector((z, z))),
                  (1, Vector((z, z + y))),
                  (0, Vector((z + x, z + y))),
                  (4, Vector((z + x, z)))),
                 ((6, Vector((0, z))),
                  (2, Vector((0, z + y))),
                  (1, Vector((z, z + y))),
                  (5, Vector((z, z)))),
                 ((6, Vector((z, 0))),
                  (5, Vector((z, z))),
                  (4, Vector((z + x, z))),
                  (7, Vector((z + x, 0)))),
                 ((7, Vector((z + z + x, z))),
                  (3, Vector((z + z + x, z + y))),
                  (2, Vector((z + x + z + x, z + y))),
                  (6, Vector((z + x + z + x, z)))),
                 ((4, Vector((z + x, z))),
                  (0, Vector((z + x, z + y))),
                  (3, Vector((z + z + x, z + y))),
                  (7, Vector((z + z + x, z)))),
                 ((1, Vector((z + x + x, 0))),
                  (2, Vector((z + x + x, z))),
                  (3, Vector((z + x, z))),
                  (0, Vector((z + x, 0))))])


def rotation_matrix_from_euler(rot_x, rot_y, rot_z):
    rot_mat = (Matrix.Rotation(rot_y / 180 * pi, 3, (0, 1, 0)) *
               Matrix.Rotation(rot_z / 180 * pi, 3, (0, 0, 1)) *
               Matrix.Rotation(rot_x / 180 * pi, 3, (1, 0, 0)))
    return rot_mat


def read_tcn(context, file_p, op):
    with ZipFile(file_p, 'r') as tcnfile:
        factory = ElementFactory(
            tcnfile, Techne=Techne, Model=Model, Shape=Shape, Piece=Piece)
        scene = context.scene
        with tcnfile.open('model.xml', 'rU') as modelxml:
            def handle(parent, xml_parent):
                if parent is None and len(xml_parent):
                    # this means we don't want to pursue the tree from here
                    return False
                for element in xml_parent:
                    local_parent = parent.append(element, factory)
                    if not handle(local_parent, element):
                        Reporter.warning(
                            "Ignored handling of child {childtag} on element {parenttag}", childtag=element.tag, parenttag=xml_parent.tag)
                return True
            xml_root = ElementTree.parse(modelxml).getroot()
            techne = factory(xml_root)
            handle(techne, xml_root)
            objs = techne.apply()
            for obj in objs:
                scene.objects.link(obj)
            scene.update()
    op.report(
        {'INFO'}, "Successfully imported techne-file {f}".format(f=file_p))
    return {'FINISHED'}
