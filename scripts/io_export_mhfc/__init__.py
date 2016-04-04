# ##### BEGIN GPL LICENSE BLOCK #####
#
#  This program is free software; you can redistribute it and/or
#  modify it under the terms of the GNU General Public License
#  as published by the Free Software Foundation; either version 2
#  of the License, or (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software Foundation,
#  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
#
# ##### END GPL LICENSE BLOCK #####

import bpy

from . import properties, layout, operators

bl_info = {
    "name": "Export: Minecraft Model (.mcmd)",
    "description": "Export monsters and objects to Minecraft",
    "author": "Martin Molzer",
    "version": (0, 6),
    "blender": (2, 70, 0),
    "location": "File > Export > Monster Hunter Frontier Craft (.mcmd)",
    "warning": "",
    "wiki_url": "",
    "tracker_url": "",
    "category": "Import-Export",
}


def register():
    bpy.utils.register_module(__name__)
    # + add scene props
    properties.register()
    layout.register()


def unregister():
    bpy.utils.unregister_module(__name__)
    # + remove scene props
    properties.unregister()
    layout.unregister()

if __name__ == "__main__":
    register()
