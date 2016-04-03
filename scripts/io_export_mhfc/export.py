from collections import defaultdict
from contextlib import ExitStack
import bmesh
import bpy
import os
import re
import struct

from .utils import Reporter, asset_to_dir, openw_save, extract_safe


class Writer(object):
    _file_h = None

    def __init__(self, file_h):
        self._file_h = file_h

    def write_bytes(self, bytestring):
        """Writes a byte string like it is to the file
        @return: 
        """
        self._file_h.write(bytestring)

    def write_string(self, string):
        """Writes a String to a file
        """
        self.write_bytes(string.encode("utf-8") + b'\x00')
        return self

    def write_packed(self, fmt, *args):
        """Packs the given data into the given bytebuffer using the given format
        """
        self.write_bytes(struct.pack(fmt, *args))
        return self


class Bone(object):

    def __init__(self, bone):
        """
        bone Bone: can't be None, the bone this is initialized from
        """
        if bone is None:
            Reporter.fatal("(Bone) bone is None")
        self.name = bone.name
        mat = bone.matrix_local
        if bone.parent is not None:
            # this is a little quick fix we have to do, don't ask me...
            mat = bone.parent.matrix_local.inverted() * mat
        self.quat = mat.to_quaternion()
        self.translate = mat.translation

    def dunp(self, writer):
        q = self.quat
        t = self.translate
        writer.write_string(self.name)
        writer.write_packed(">7f", q.x, q.y, q.z, q.w, t.x, t.y, t.z)


class Point(object):

    def __init__(self, loop, uv_layer, deform_layer, arm_vgroup_idxs):
        """
        loop Loop: Can't be none, the loop this points represents
        uv_layer LLayerItem: Can't be None, the uv_layer we export
        deform_layer VLayerItem: Can be None, indicates that no bindings
                                                         should be exported
        arm_vgroup_idxs list: Can't be None, value at index i represents the index of the
                                                  arm.bones[i]'s vertex group in object
        """
        if loop is None:
            Reporter.fatal("(Point) Loop is None")
        vtx = loop.vert
        self.coords = vtx.co
        self.normal = vtx.normal
        self.uv = loop[uv_layer].uv
        self.bindings = []
        # calculate bindings
        if deform_layer is None:
            return

        def insert_into_binds(bind):
            """
            bind tuple: Can't be None, (idx, value) of binding
            """
            if bind is None:
                Reporter.fatal("(Point) Inserted tupple is None")
            self.bindings.append(bind)
        # iterate over all bones (as indices)
        for idx, vgroup_idx in enumerate(arm_vgroup_idxs):
            if vgroup_idx == -1:
                # bone has no vertex group
                continue
            try:
                value = vtx[deform_layer][vgroup_idx]
            except KeyError:
                # no binding to this bone
                pass
            else:
                if value > 0:
                    insert_into_binds((idx, value))

    def dump(self, writer):
        binds = sorted(self.bindings, key=lambda x: x[1])[-4:]
        co = self.coords
        norm = self.normal
        uv = self.uv
        writer.write_packed(
            ">8f" + "Bf" * len(binds),
            co.x, co.y, co.z,
            norm.x, norm.y, norm.z,
            uv.x, 1 - uv.y,
            *[v for bind in binds for v in bind])
        if len(binds) < 4:
            writer.write_bytes(b'\xFF')

    def __eq__(self, other):
        epsilon = 0.001
        return (self.bindings == other.bindings and
                (self.uv - other.uv).length < epsilon and
                (self.coords - other.coords).length < epsilon and
                (self.normal - other.normal).length < epsilon)

    def __str__(self):
        return "Point(co={co}, normal={norm}, uv={uv}, bindings={binds})".format(
            co=self.coords,
            norm=self.normal,
            uv=self.uv,
            binds=self.bindings)


class Part(object):

    def __init__(self, group, options):
        """
        group Group: can be None
                in case of None, it is the default group
        """
        img = options.def_image
        if group is None:
            self.name = options.def_group_name
        elif group.image in bpy.data.images:
            self.name = group.name
            img = group.image
        else:
            self.name = group.name
            Reporter.warning("Group {name} has an invalid image ({img}) assigned. Defaulted to {default}",
                             name=self.name, img=group.image, default=options.def_image)
            img = options.def_image.name
        self.image = bpy.data.images[img]
        self.img_location = options.texpath.format(
            modid=options.mod_id,
            modelname=options.modelname,
            texname=bpy.path.ensure_ext(img, '.png')[:-4])
        # maps vertices to (point, list-index) pair-list
        self.point_map = defaultdict(lambda: [])
        # a full list of all points
        self.points = []
        self.indices = []

    def append_face(self, face, uv_layer, deform_layer, arm_vgroup_idxs):
        """
        uv_layer LLayerItem: Can't be None, the uv_layer we export
        deform_layer VLayerItem: Can be None, indicates that no bindings
                                                         should be exported
        arm_vgroup_idxs list: Can't be None, value at index i represents the index of the
                                                  arm.bones[i]'s vertex group in object
        """
        def key_from_loop(l):
            return l.vert.index
        for loop in face.loops:
            key = key_from_loop(loop)
            # append points or reuse existant one's
            new_point = Point(loop, uv_layer, deform_layer, arm_vgroup_idxs)
            index = -1
            known_points = self.point_map[key]
            for point, idx in known_points:
                if point == new_point:
                    index = idx
                    break
            else:
                index = len(self.points)
                self.points.append(new_point)
                known_points.append((new_point, index))
            assert index >= 0
            self.indices.append(index)

    def dump(self, writer):
        points = self.points
        p_len = len(self.points)
        idxs = self.indices
        idxs_len = len(idxs)
        if idxs_len % 3:
            Reporter.fatal("(Part) number of indices not divisible by 3")
        tris = idxs_len // 3
        if tris >= 2**16:
            Reporter.error(
                "(Part) too many tris in part {name}", name=self.name)
        writer.write_packed(">2H", p_len, tris)
        writer.write_string(self.name)
        writer.write_string(self.img_location)
        for point in points:
            point.dump(writer)
        writer.write_packed(">{idxs}H".format(idxs=idxs_len), *idxs)


class Animation(object):

    def __init__(self, fcurve, start):
        self.extrapolation_mode = 'CONSTANT'
        self.offset = start
        if fcurve is None:
            self.points = []
        else:
            self.points = [keyf for keyf in fcurve.keyframe_points]
            self.extrapolation_mode = fcurve.extrapolation

    def dump(self, writer):
        interpolations = {'CONSTANT': 8, 'LINEAR': 9, 'BEZIER': 10}
        extrapolations = {'CONSTANT': 16, 'LINEAR': 17}

        points = self.points

        def write_point(co):
            writer.write_packed(">2f", co[0] - self.offset, co[1])
        anim_len = len(points)
        if anim_len > 2**16 - 1:
            Reporter.error("Too many keyframes in fcurve to export")
        # animation length
        writer.write_packed(">H", anim_len)
        if anim_len == 0:
            return
        write_point(points[0].co)
        # TUNE IN
        writer.write_packed(">B", 0)  # = LINEAR
        # POINTS
        for left, right in zip(points[:], points[1:]):
            write_point(right.co)
            raw_mode = right.interpolation
            interpolation_code = extract_safe(
                interpolations, raw_mode, "Unknown interpolation mode {item}")
            writer.write_packed(">B", interpolation_code)
            if raw_mode == 'BEZIER':
                write_point(left.handle_right)
                write_point(right.handle_left)
        # TUNE OUT
        extrapolation_code = extract_safe(
            extrapolations, self.extrapolation_mode, "Unknown extrapolation mode {item}")
        writer.write_packed(">B", extrapolation_code)
        if self.extrapolation_mode == 'LINEAR':
            write_point(points[-1].handle_right)


class BoneAction(object):

    def __init__(self, name, curves, offset):
        loc_curves, rot_curves, scale_curves = curves
        self.name = name
        self.loc_x = Animation(loc_curves[0], offset)
        self.loc_y = Animation(loc_curves[1], offset)
        self.loc_z = Animation(loc_curves[2], offset)

        self.rot_w = Animation(rot_curves[0], offset)
        self.rot_x = Animation(rot_curves[1], offset)
        self.rot_y = Animation(rot_curves[2], offset)
        self.rot_z = Animation(rot_curves[3], offset)

        self.scale_x = Animation(scale_curves[0], offset)
        self.scale_y = Animation(scale_curves[1], offset)
        self.scale_z = Animation(scale_curves[2], offset)

    def dump(self, writer):
        writer.write_string(self.name, writer)
        self.loc_x.dump(writer)
        self.loc_y.dump(writer)
        self.loc_z.dump(writer)

        self.rot_x.dump(writer)
        self.rot_y.dump(writer)
        self.rot_z.dump(writer)
        self.rot_w.dump(writer)

        self.scale_x.dump(writer)
        self.scale_y.dump(writer)
        self.scale_z.dump(writer)


class MeshExportOptions(object):
    mod_id = None
    dirpath = None
    modelpath = None
    texpath = None

    obj = None
    arm = None
    uv_layer = None
    version = None

    artist = None
    modelname = None
    def_group_name = None
    def_image = None

    export_tex = None


def sort_bones(arm):
    if arm is None:
        return []
    return sorted(arm.bones, key=lambda b: b.name)


def export_mesh_v1(context, options, writer):
    # extract used values
    obj = options.obj
    # the object we export
    with ExitStack() as stack:
        if context.mode == 'EDIT_MESH':
            bmc = bmesh.from_edit_mesh(obj.data)
            stack.callback(bmesh.update_edit_mesh, obj.data)
            bm = bmc.copy()
            stack.callback(bm.free)
        else:
            bm = bmesh.new()
            stack.callback(bm.free)
            bm.from_mesh(obj.data)
        bmesh.ops.triangulate(bm, faces=bm.faces)
        # the armature to that object
        arm = options.arm
        # for each material we make one part
        part_dict = {}
        # checked
        uv_layer = bm.loops.layers.uv[options.uv_layer.name]
        # could be None
        deform_layer = bm.verts.layers.deform.active
        # sorted bones
        sorted_bones = sort_bones(arm)
        # never none
        arm_vgroup_idxs = [
            obj.vertex_groups.find(bone.name) for bone in sorted_bones]

        g_layer = None
        if 'MCRenderGroupIndex' in bm.faces.layers.int:
            g_layer = bm.faces.layers.int['MCRenderGroupIndex']
        groups = obj.data.mcprops.render_groups
        for face in bm.faces:
            g_idx = face[g_layer] - 1 if g_layer is not None else -1
            group = groups[g_idx] if g_idx >= 0 and g_idx < len(
                groups) else None
            if group not in part_dict:
                part_dict.update({group: Part(group, options)})
            part_dict[group].append_face(
                face, uv_layer, deform_layer, arm_vgroup_idxs)
        if len(part_dict) > 0xFF:
            Reporter.error("Too many parts")
        bones = [Bone(bone) for bone in sorted_bones]
        if len(bones) > 0xFF:
            Reporter.error("Too many bones")
        bone_parents = [next(i for i, c in enumerate(sorted_bones) if c == b.parent, 0xFF) & 0xFF for b in sorted_bones]

        # write the stuff
        writer.write_packed(">I", 1)
        writer.write_packed(">2B", len(part_dict), len(bones))
        for part in part_dict.values():
            part.dump(writer)
        for bone in bones:
            bone.dump(writer)
        writer.write_packed(">{nums}B".format(nums=len(bones)), *bone_parents)

        if options.export_tex:
            settings = context.scene.render.image_settings
            settings.file_format = 'PNG'
            settings.color_mode = 'RGBA'
            for img, path in set([(p.image, p.img_location) for p in part_dict.values()]):
                ext_path =\
                    os.path.join(
                        options.dirpath,
                        asset_to_dir(path))
                img.save_render(
                    bpy.path.abspath(ext_path), scene=context.scene)

    summary = 'Exported with ({numparts} parts, {numbones} bones)'.format(
        numparts=len(part_dict),
        numbones=len(bones)
    )
    Reporter.info(summary)

# exporters also have to write their version number
known_mesh_exporters = {"V1": export_mesh_v1,
                        "V2": export_mesh_v2}


def export_mesh(context, options):
    scene = context.scene
    # Write file header
    bitmask = 0xFFFFFFFF
    uuid_vec = scene.mcprops.uuid
    # Write to file
    modelpath = options.modelpath.format(
        modid=options.mod_id,
        modelname=options.modelname)
    filepath = os.path.join(
        options.dirpath,
        asset_to_dir(modelpath))
    with openw_save(filepath, 'wb') as file_h:
        writer = Writer(file_h)
        writer.write_bytes(b'MHFC MDL')
        writer.write_packed(">4I",
                            uuid_vec[0] & bitmask,
                            uuid_vec[1] & bitmask,
                            uuid_vec[2] & bitmask,
                            uuid_vec[3] & bitmask)
        writer.write_string(options.artist)
        try:
            known_mesh_exporters[options.version](context, options, writer)
        except (KeyError, NotImplementedError) as ex:
            Reporter.fatal(
                "Version {v} is not implemented yet", v=options.version)


def export_skl_v1(context, options, writer):
    with ExitStack() as stack:
        # the armature to that object
        arm = options.arm
        # sorted bones
        sorted_bones = sort_bones(arm)

        bones = ([] if sorted_bones is None else
                 [Bone(bone) for bone in sorted_bones])
        if len(bones) > 0xFF:
            Reporter.error("Too many bones")
        bone_parents = ([] if sorted_bones is None else
                        [sorted_bones.find(b.parent.name) & 0xFF if b.parent is not None else 255 for b in sorted_bones])

        # write the stuff
        writer.write_packed(">I", 1)
        writer.write_packed(">B", len(bones))
        for bone in bones:
            bone.dump(writer)
        writer.write_packed(">{nums}B".format(nums=len(bones)), *bone_parents)

    Reporter.info('Exported {numbones} bones'.format(numbones=sorted_bones))


def export_action(filepath, action, offset, artist, armature):
    bone_data_re = re.compile(
        "pose\.bones\[\"(.*)\"\]\.(rotation_quaternion|location|scale)")
    bone_dict = defaultdict(lambda: ([None] * 3, [None] * 4, [None] * 3))
    armBones = sort_bones(armature)
    curves = 0
    for curve in action.fcurves:
        match = bone_data_re.match(curve.data_path)
        if match is None:
            continue
        bone, key = match.groups()
        if bone not in armBones:
            continue
        if key == 'location' and not armBones[bone].use_connect:
            bone_dict[bone][0][curve.array_index] = curve
            curves += 1
        elif key == 'rotation_quaternion':
            bone_dict[bone][1][curve.array_index] = curve
            curves += 1
        elif key == 'scale':
            bone_dict[bone][2][curve.array_index] = curve
            curves += 1
    bone_count = len(bone_dict)
    if bone_count > 255:
        Reporter.error("Too many bones to export")
    with open(filepath, 'wb') as file_h:
        writer = Writer(file_h)
        writer.write_bytes(b'MHFC ANM')
        writer.write_string(artist)
        writer.write_packed(">B", bone_count)
        for bone in bone_dict:
            BoneAction(bone, bone_dict[bone], offset).dump(writer)
    summary = 'Exported {bones} bones with {curves} animated values'.format(
        bones=bone_count,
        curves=curves
    )
    Reporter.info(summary)
