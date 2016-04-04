import bpy

from bpy.types import Panel, Menu, UIList, Header

from .operators import ObjectExporter, ArmAnimationExporter, ArmatureUpdater,\
    AddRenderGroup, RemoveRenderGroup, AddFacesToGroup, SelectGroup,\
    UpdateGroupsVisual, ImportTechne


class RenderGroupUIList(UIList):

    def draw_item(self, context, layout, data, item, icon, active_data, active_propname):
        if self.layout_type in {'DEFAULT', 'COMPACT'}:
            if item:
                row = layout.row()
                row.prop(item, "name", text="", emboss=False, icon_value=icon)
                if item.image not in bpy.data.images:
                    row.label(icon="ERROR")
            else:
                layout.label(text="", translate=False, icon_value=icon)
        # 'GRID' layout type should be as compact as possible (typically a single icon!).
        elif self.layout_type in {'GRID'}:
            layout.alignment = 'CENTER'
            layout.label(text="", icon_value=icon)


class AnimationExportMenu(Menu):
    bl_label = "Export Action"
    bl_idname = "animation.mc_export"

    def draw(self, context):
        layout = self.layout
        sce = context.scene
        if context.space_data.mode == 'ACTION':
            row = layout.row()
            op = row.operator(ArmAnimationExporter.bl_idname)
            # op.armature = context.object
            op.offset = sce.frame_start
            if not context.space_data.action:
                row.enabled = False
            else:
                op.arm_action = context.space_data.action.name


class AnimationExportHeader(Header):
    bl_label = "Animation Export Menu"
    bl_idname = "animation.mc_export"
    bl_space_type = "DOPESHEET_EDITOR"

    def draw(self, context):
        layout = self.layout
        layout.menu(AnimationExportMenu.bl_idname)


class ObjectPropertiesPanel(Panel):
    # MC Panel under Object
    bl_label = "Monster Hunter Frontier Craft"
    bl_idname = "object.mc_export"
    bl_region_type = "WINDOW"
    bl_space_type = "PROPERTIES"
    bl_context = "object"

    @classmethod
    def poll(cls, context):
        return context.object is not None and context.object.type == 'MESH'

    def draw(self, context):
        # setup vars
        obj = context.object
        data = obj.data
        meshprops = data.mcprops
        layout = self.layout
        # check for invalidity: #poll
        # Properties of this object
        box = layout.box()
        box.label(text="Object properties")
        box.prop(meshprops, 'artist', text="Artist name")
        box.prop(meshprops, 'name', text="Model Name")
        if not meshprops.name:
            box.label(text="Select model name", icon="ERROR")
            box.separator()
        box.prop_search(meshprops, 'armature', meshprops,
                        'poss_arms', text="Armature", icon="ARMATURE_DATA")
        if meshprops.armature and meshprops.armature not in meshprops.poss_arms:
            box.label(text="Invalid Armature", icon="ERROR")
            box.separator()
        box.operator(ArmatureUpdater.bl_idname)
        box.prop_search(
            meshprops, 'uv_layer', data, 'uv_layers', text="UV Layer", icon="GROUP_UVS")
        if not meshprops.uv_layer:
            box.label(text="Select a UV map.", icon="ERROR")
            box.separator()
        elif meshprops.uv_layer not in data.uv_layers:
            box.label(text="Invalid UV Map", icon="ERROR")
            box.separator()
        box = box.box()
        box.label(text="Default group")
        box.prop(meshprops.default_group, 'name', text="Name")
        if not meshprops.default_group.name:
            box.label(text="Select a default group name.", icon="ERROR")
            layout.separator()
        elif meshprops.default_group.name in meshprops.render_groups:
            box.label(text="Name collision with existing group.", icon="ERROR")
            layout.separator()
        box.prop_search(meshprops.default_group, 'image', bpy.data,
                        'images', text="Texture", icon="IMAGE_DATA")
        if not meshprops.default_group.image:
            box.label(text="Select default texture.", icon="ERROR")
            layout.separator()
        elif meshprops.default_group.image not in bpy.data.images:
            layout.label(text="Invalid image texture", icon="ERROR")
            layout.separator()


class MeshDataPanel(Panel):
    # MC Panel under Object
    bl_idname = "object.mc_meshdata"
    bl_label = "Minecraft Render Groups"
    bl_region_type = "WINDOW"
    bl_space_type = "PROPERTIES"
    bl_context = "data"

    @classmethod
    def poll(cls, context):
        return context.object is not None and context.object.type == 'MESH'

    def draw(self, context):
        layout = self.layout
        props = context.object.data.mcprops
        groups = props.render_groups
        active_idx = props.active_render_group

        row = layout.row()
        row.template_list(
            listtype_name='RenderGroupUIList',
            dataptr=props,
            propname='render_groups',
            active_dataptr=props,
            active_propname='active_render_group',
            rows=2,
            type='DEFAULT',
        )
        col = row.column(align=True)
        col.operator(
            AddRenderGroup.bl_idname,
            text="", icon='ZOOMIN')
        col.operator(
            RemoveRenderGroup.bl_idname,
            text="", icon='ZOOMOUT')
        if active_idx >= 0 and active_idx < len(groups):
            active_g = groups[active_idx]
            layout.prop_search(
                active_g, 'image', bpy.data, 'images', text="Image", icon="IMAGE_DATA")
            if not active_g.image:
                layout.label(
                    text="Select an imagetexture for active group", icon="ERROR")
            elif active_g.image not in bpy.data.images:
                layout.label(text="Invalid image texture", icon="ERROR")
        if context.mode == 'EDIT_MESH':
            row = layout.row()
            row.operator(AddFacesToGroup.bl_idname)
            row.operator(SelectGroup.bl_idname)
            layout.operator_menu_enum(UpdateGroupsVisual.bl_idname, 'mode')


class AnimationExportPanel(Panel):
    # MC Panel under Object
    bl_space_type = "VIEW_3D"
    bl_region_type = "TOOLS"
    bl_category = "Export"
    bl_label = "Minecraft"
    bl_idname = "view3d.mcanm_export"

    @classmethod
    def poll(cls, context):
        return context.object is not None and context.object.type == 'ARMATURE'

    def draw(self, context):
        layout = self.layout
        obj = context.object
        sce = context.scene
        row = layout.row()
        row.operator_context = 'INVOKE_DEFAULT'
        op = row.operator(ArmAnimationExporter.bl_idname)
        op.offset = sce.frame_start
        if obj.animation_data is not None and obj.animation_data.action is not None:
            action = obj.animation_data.action
            op.arm_action = action.name
            op.armature = obj.name
            layout.prop(action.mcprops, 'artist')
            op.artist = action.mcprops.artist
        else:
            row.enabled = False


def export_func(self, context):
    self.layout.operator(
        ObjectExporter.bl_idname, text="Minecraft Animated models (.mcmd)")


def import_func(self, context):
    self.layout.operator(
        ImportTechne.bl_idname, text="Techne Models (.tcn)")


def register():
    bpy.types.INFO_MT_file_export.append(export_func)
    bpy.types.INFO_MT_file_import.append(import_func)


def unregister():
    bpy.types.INFO_MT_file_export.remove(export_func)
    bpy.types.INFO_MT_file_import.remove(import_func)
