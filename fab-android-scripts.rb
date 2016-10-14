class FabAndroidScripts < Formula
  desc ""
  homepage "https://github.com/thefabulous/fab-android-scripts"
  url "https://github.com/thefabulous/fab-android-scripts/archive/master.tar.gz"
  version "1.1.3"
  sha256 "324ce5d98c079a2feacbcde6f3e541599871910f9acebebbe9728353dd6fa12b"

  depends_on :x11

  def install
    bin.install 'src/devtools.groovy' => 'devtools'
    bin.install 'src/adbwifi.groovy' => 'adbwifi'
    bin.install 'src/adbscreenrecord.groovy' => 'adbscreenrecord'

    system "./configure", "--disable-debug",
                          "--disable-dependency-tracking",
                          "--disable-silent-rules",
                          "--prefix=#{prefix}"
    system "make", "install"
  end
end
