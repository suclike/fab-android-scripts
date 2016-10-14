require "formula"

class FabAndroidScripts < Formula
  desc "Fab Internal Android Script Tools"
  homepage "https://github.com/thefabulous/fab-android-scripts"
  url "https://github.com/thefabulous/fab-android-scripts/archive/master.tar.gz"
  version "1.1.3"
  sha256 ""
  head "https://github.com/thefabulous/fab-android-scripts.git"

  depends_on "groovy"

  def install
    bin.install 'src/devtools.groovy' => 'devtools'
    bin.install 'src/adbwifi.groovy' => 'adbwifi'
    bin.install 'src/adbscreenrecord.groovy' => 'adbscreenrecord'
  end
end
