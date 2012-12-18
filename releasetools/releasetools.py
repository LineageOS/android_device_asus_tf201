# Copyright (C) 2012 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import common
import os, sys

LOCAL_DIR = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
DEV_OUT_DIR = os.path.abspath(os.path.join(LOCAL_DIR, '../../../out/target/product/tf201/'))

def FullOTA_InstallEnd(info):
  blob_path = os.path.join(DEV_OUT_DIR, "boot.img.LNX")
  try:
    tegra_blob = common.File.FromLocalFile("boot.blob", blob_path)
  except KeyError:
    print "No LNX blob found !"
  else:
    WriteTegraBlob(info, tegra_blob)


def WriteTegraBlob(info, tegra_blob):
  print "Adding LNX blob to zip ..."
  common.ZipWriteStr(info.output_zip, "boot.blob", tegra_blob.data)
  fstab = info.info_dict["fstab"]
  info.script.Print("Writing blob to staging...")
  info.script.AppendExtra('''package_extract_file("boot.blob", "%s");''' %
                          (fstab["/staging"].device,))

