import AppKit
import Foundation

func makePNG(_ size: Int, _ draw: (CGFloat) -> Void, to path: String) throws {
    let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: size, pixelsHigh: size * 9 / 16, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    NSGraphicsContext.saveGraphicsState()
    let context = NSGraphicsContext(bitmapImageRep: rep)!
    NSGraphicsContext.current = context
    context.imageInterpolation = .high
    let h = CGFloat(size) * 9 / 16
    NSColor(calibratedRed: 0.035, green: 0.055, blue: 0.085, alpha: 1).setFill()
    NSBezierPath(rect: NSRect(x: 0, y: 0, width: CGFloat(size), height: h)).fill()
    draw(CGFloat(size))
    context.flushGraphics()
    NSGraphicsContext.restoreGraphicsState()
    let data = rep.representation(using: .png, properties: [:])!
    try data.write(to: URL(fileURLWithPath: path))
}

let cyan = NSColor(calibratedRed: 0.22, green: 0.84, blue: 0.95, alpha: 1)
let white = NSColor(calibratedRed: 0.94, green: 0.96, blue: 0.99, alpha: 1)
let icon = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 512, pixelsHigh: 512, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
NSGraphicsContext.saveGraphicsState()
let ic = NSGraphicsContext(bitmapImageRep: icon)!
NSGraphicsContext.current = ic
let bounds = NSRect(x: 0, y: 0, width: 512, height: 512)
NSColor(calibratedRed: 0.035, green: 0.055, blue: 0.085, alpha: 1).setFill()
NSBezierPath(roundedRect: bounds.insetBy(dx: 8, dy: 8), xRadius: 108, yRadius: 108).fill()
cyan.setStroke()
let house = NSBezierPath()
house.lineWidth = 25
house.lineCapStyle = .round
house.lineJoinStyle = .round
house.move(to: NSPoint(x: 100, y: 265)); house.line(to: NSPoint(x: 256, y: 390)); house.line(to: NSPoint(x: 412, y: 265)); house.line(to: NSPoint(x: 412, y: 105)); house.line(to: NSPoint(x: 100, y: 105)); house.close(); house.stroke()
cyan.setFill()
for x in [160.0, 270.0] { for y in [150.0, 215.0] { NSBezierPath(roundedRect: NSRect(x: x, y: y, width: 58, height: 42), xRadius: 8, yRadius: 8).fill() } }
NSGraphicsContext.restoreGraphicsState()
try icon.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: "app/src/main/res/drawable-nodpi/aft_home_icon.png"))

try makePNG(960, { width in
    let h = width * 9 / 16
    let s = width / 960
    cyan.setStroke()
    let p = NSBezierPath(); p.lineWidth = 16*s; p.lineCapStyle = .round; p.lineJoinStyle = .round
    p.move(to: NSPoint(x: 90*s,y: 145*s)); p.line(to: NSPoint(x: 180*s,y: 225*s)); p.line(to: NSPoint(x: 270*s,y: 145*s)); p.line(to: NSPoint(x: 270*s,y: 45*s)); p.line(to: NSPoint(x: 90*s,y: 45*s)); p.close(); p.stroke()
    cyan.setFill()
    for x in [125.0, 190.0] { for y in [65.0, 105.0] { NSBezierPath(roundedRect: NSRect(x: x*s, y: y*s, width: 28*s, height: 23*s), xRadius: 4*s, yRadius: 4*s).fill() } }
    let attrs: [NSAttributedString.Key: Any] = [.font: NSFont.systemFont(ofSize: 100*s, weight: .semibold), .foregroundColor: white]
    ("AFT" as NSString).draw(at: NSPoint(x: 350*s, y: 94*s), withAttributes: attrs)
    let subAttrs: [NSAttributedString.Key: Any] = [.font: NSFont.systemFont(ofSize: 46*s, weight: .medium), .foregroundColor: cyan]
    ("HOME" as NSString).draw(at: NSPoint(x: 360*s, y: 42*s), withAttributes: subAttrs)
    _ = h
}, to: "app/src/main/res/drawable-nodpi/aft_home_banner.png")
