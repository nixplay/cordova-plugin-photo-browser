//
//  ImageViewer.h
//  Helper
//
//  Created by Calvin Lai on 7/11/13.   
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import "MWPhotoBrowser.h"
#import <PopupDialog/PopupDialog-Swift.h>
#import <IQKeyboardManager/IQKeyboardManager.h>
#import <IQKeyboardManager/IQTextView.h>
#import <SDWebImage/SDWebImageManager.h>
@interface PhotoBrowserPlugin : CDVPlugin <MWPhotoBrowserDelegate,UINavigationControllerDelegate, CAAnimationDelegate, UITextViewDelegate, SDWebImageManagerDelegate> {
    
    NSMutableDictionary* _callbackIds;
    NSMutableDictionary* _actionSheetDicArray;
    
    UIBarButtonItem *_rightBarbuttonItem;
    UIBarButtonItem *_addAttachButton;
    UIBarButtonItem *_leftBarbuttonItem;
    IQTextView *_textView;
    NSInteger _id;
    NSString *_type;
    NSString* _dateString;
    BOOL _readOnly;
    NSString *_ctaText;
    NSString *_action;
    NSString *_collectionId;
    NSInteger _currentCaptionIndex;
}
@property (copy)   NSString* callbackId;
@property (nonatomic, retain) NSMutableArray *photos;
@property (nonatomic, retain) NSArray *thumbs;
@property (nonatomic, retain) NSMutableArray *data;
@property (nonatomic, retain) NSMutableArray *selections;
@property (nonatomic, retain) UIToolbar *toolBar;
@property (nonatomic, retain) UIColor *titleViewColor;
@property (nonatomic, retain) UINavigationController *navigationController;
@property (nonatomic, retain) MWGridViewController* gridViewController;
@property (nonatomic, retain) MWPhotoBrowser *browser;
@property (nonatomic, retain) NSString *albumName;
@property (nonatomic, retain) PopupDialog *dialogView;
@property (nonatomic, retain) NSMutableDictionary *HTTPResponseHeaderOrientations;
- (void)showGallery:(CDVInvokedUrlCommand*)command;

@end

