//
//  RNMailCompose.swift
//  DropCard
//
//  Created by Joon Ho Cho on 4/30/17.
//

import Foundation
import MobileCoreServices
import MessageUI


@objc(RNMailCompose)
class RNMailCompose: NSObject, MFMailComposeViewControllerDelegate {
  var resolve: RCTPromiseResolveBlock?
  var reject: RCTPromiseRejectBlock?
  
  @objc func constantsToExport() -> [String: Any] {
    return [
      "name": "RNMailCompose",
    ]
  }
  
  @objc func canSendMail(_ resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
    return resolve(MFMailComposeViewController.canSendMail())
  }
  
  func textToData(utf8: String?, base64: String?) -> Data? {
    if let utf8 = utf8 {
      return utf8.data(using: .utf8)
    }
    if let base64 = base64 {
      return Data(base64Encoded: base64, options: .ignoreUnknownCharacters)
    }
    return nil
  }
  
  func toFilename(filename: String?, ext: String?) -> String? {
    if let ext = ext {
      return (filename ?? UUID().uuidString) + ext
    }
    return nil
  }
  
  @objc func send(_ data: [String: Any], resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) {
    if !MFMailComposeViewController.canSendMail() {
      reject("cannotSendMail", "Cannot send mail", nil)
      return
    }
    
    let vc = MFMailComposeViewController()
    
    if let value = data["subject"] as? String {
      vc.setSubject(value)
    }
    if let value = data["toRecipients"] as? [String] {
      vc.setToRecipients(value)
    }
    if let value = data["ccRecipients"] as? [String] {
      vc.setCcRecipients(value)
    }
    if let value = data["bccRecipients"] as? [String] {
      vc.setBccRecipients(value)
    }
    if let value = data["body"] as? String {
      vc.setMessageBody(value, isHTML: false)
    }
    if let value = data["html"] as? String {
      vc.setMessageBody(value, isHTML: true)
    }
    
    if let value = data["attachments"] as? [[String: String]] {
      for dict in value {
        if let data = textToData(utf8: dict["text"], base64: dict["data"]), let mimeType = dict["mimeType"], let filename = toFilename(filename: dict["filename"], ext: dict["ext"]) {
          vc.addAttachmentData(data, mimeType: mimeType, fileName: filename)
        }
      }
    }
    
    vc.mailComposeDelegate = self
    
    if present(viewController: vc) {
      self.resolve = resolve
      self.reject = reject
    } else {
      reject("failed", "Could not present view controller", nil)
    }
  }
  
  func mailComposeController(_ controller: MFMailComposeViewController, didFinishWith result: MFMailComposeResult, error: Error?) {
    switch (result) {
    case .cancelled:
      reject?("cancelled", "Operation has been cancelled", nil)
      break
    case .sent:
      resolve?("sent")
      break
    case .saved:
      reject?("saved", "Draft has been saved", nil)
      break
    case .failed:
      reject?("failed", "Operation has failed", nil)
      break
    }
    resolve = nil
    reject = nil
    
    controller.dismiss(animated: true, completion: nil)
  }
  
  func getTopViewController(window: UIWindow?) -> UIViewController? {
    if let window = window {
      var top = window.rootViewController
      while true {
        if let presented = top?.presentedViewController {
          top = presented
        } else if let nav = top as? UINavigationController {
          top = nav.visibleViewController
        } else if let tab = top as? UITabBarController {
          top = tab.selectedViewController
        } else {
          break
        }
      }
      return top
    }
    return nil
  }
  
  func present(viewController: UIViewController) -> Bool {
    if let topVc = getTopViewController(window: UIApplication.shared.keyWindow) {
      topVc.present(viewController, animated: true, completion: nil)
      return true
    }
    return false
  }
}

