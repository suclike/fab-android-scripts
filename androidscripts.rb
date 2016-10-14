class AndroidScripts < Formula
  desc ""
  homepage "https://github.com/thefabulous/android-scripts"
  url "https://github.com/thefabulous/android-scripts/archive/1.1.2.tar.gz"
  version "1.1.2"
  sha256 "5aaa7ca990360a0931aa3a63679d9e554f942f2dc159b38f1242b7862ab76682"

  def install
    bin.install 'src/devtools.groovy' => 'devtools'
    bin.install 'src/adbwifi.groovy' => 'adbwifi'
    bin.install 'src/adbscreenrecord.groovy' => 'adbscreenrecord'
  end
end