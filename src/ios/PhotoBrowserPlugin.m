//
//  ImageViewer.m
//  Helper
//
//  Created by Calvin Lai on 7/11/13.
//
//

#import "PhotoBrowserPlugin.h"
#import "MWPhotoBrowser.h"
#import "MWGridViewController.h"
#import "TextInputViewController.h"
#import <Cordova/CDVViewController.h>
#import "MKActionSheet.h"
#import "UIImage+MWPhotoBrowser.h"
#import <Cordova/CDVPlugin+Resources.h>
#import <PopupDialog/PopupDialog-Swift.h>
#import <IQKeyboardManager/IQTextView.h>
#import <IQKeyboardManager/IQUITextFieldView+Additions.h>
#import <IQKeyboardManager/IQUIView+IQKeyboardToolbar.h>
#import <SDWebImage/SDWebImageManager.h>
#import <SDWebImage/SDWebImageDownloaderOperation.h>
#import <MBProgressHUD/MBProgressHUD.h>

#define MAX_CHARACTER 160
#define VIEWCONTROLLER_TRANSITION_DURATION 0.2
#define DEFAULT_ACTION_ADD @"add"
#define DEFAULT_ACTION_SELECT @"select"
#define DEFAULT_ACTION_ADDTOPLAYLIST @"addToPlaylist"
#define DEFAULT_ACTION_RENAME @"rename"
#define DEFAULT_ACTION_DELETE @"delete"
#define KEY_ALBUM @"album"
#define KEY_ACTION  @"action"
#define KEY_LABEL  @"label"
#define KEY_NAME @"name"
#define KEY_ID @"id"
#define KEY_TYPE @"type"
#define KEY_DELETEPHOTOS @"deletePhotos"
#define KEY_PHOTOS @"photos"

#define BUNDLE_UIIMAGE(imageNames) [UIImage imageNamed:[NSString stringWithFormat:@"%@.bundle/%@", NSStringFromClass([self class]), imageNames]]
#define OPTIONS_UIIMAGE BUNDLE_UIIMAGE(@"images/options.png")
#define DOWNLOADIMAGE_UIIMAGE BUNDLE_UIIMAGE(@"images/downloadCloud.png")
#define SEND_UIIMAGE BUNDLE_UIIMAGE(@"images/send.png")
#define EDIT_UIIMAGE BUNDLE_UIIMAGE(@"images/edit.png")
#define CLOSE_UIIMAGE BUNDLE_UIIMAGE(@"images/close.png")
#define BIN_UIIMAGE BUNDLE_UIIMAGE(@"images/bin.png")

#define LIGHT_BLUE_COLOR [UIColor colorWithRed:(96.0f/255.0f)  green:(178.0f/255.0f)  blue:(232.0f/255.0f) alpha:1.0]
#define IS_TYPE_ALBUM ([_type isEqualToString:KEY_ALBUM])
#define SUBTITLESTRING_FOR_TITLEVIEW(dateString) (IS_TYPE_ALBUM && ![_dateString isEqualToString:@"Unknown Date"] ) ? [NSString stringWithFormat:@"%lu %@ - %@", (unsigned long)[_photos count] , NSLocalizedString(KEY_PHOTOS,nil) , dateString] : [NSString stringWithFormat:@"%lu %@", (unsigned long)[_photos count] , NSLocalizedString(KEY_PHOTOS,nil)]


#define CDV_PHOTO_PREFIX @"cdv_photo_"
#define SELECTALL_TAG 0x31
enum Orientation {
    TOP_LEFT = 1,
    TOP_RIGHT = 2,
    BOTTOM_LEFT = 3,
    BOTTOM_RIGHT = 4,
    LEFT_TOP = 5,
    RIGHT_TOP = 6,
    LEFT_BOTTOM = 7,
    RIGHT_BOTTOM = 8,
};
@implementation PhotoBrowserPlugin
@synthesize callbackId;
@synthesize photos = _photos;
@synthesize thumbs = _thumbs;
@synthesize browser = _browser;
@synthesize data = _data;
@synthesize actionSheet = _actionSheet;
@synthesize navigationController = _navigationController;
@synthesize albumName = _name;
@synthesize gridViewController = _gridViewController;
@synthesize toolBar = _toolBar;
@synthesize HTTPResponseHeaderOrientations = _HTTPResponseHeaderOrientations;
- (NSMutableDictionary*)callbackIds {
    if(_callbackIds == nil) {
        _callbackIds = [[NSMutableDictionary alloc] init];
    }
    return _callbackIds;
}

- (void)showGallery:(CDVInvokedUrlCommand*)command {
    
    [SDWebImageManager sharedManager].delegate = self;
    self.callbackId = command.callbackId;
    [self.callbackIds setValue:command.callbackId forKey:@"showGallery"];
    
    NSDictionary *options = [command.arguments objectAtIndex:0];
    NSArray * imagesUrls = [options objectForKey:@"images"] ;
    _data = [options objectForKey:@"data"];
    _HTTPResponseHeaderOrientations = [NSMutableDictionary new];
    if(imagesUrls == nil || [imagesUrls count] <= 0 ){
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Argument \"images\" clould not be empty"];
        [pluginResult setKeepCallbackAsBool:NO];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
        return;
    }
    if( _data == nil || [_data count] == 0 || [_data count] != [imagesUrls count] ){
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Argument \"data\" clould not be empty"];
        [pluginResult setKeepCallbackAsBool:NO];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
        return;
    }
    NSMutableArray *images = [[NSMutableArray alloc] init];
    NSMutableArray *thumbs = [[NSMutableArray alloc] init];
    NSUInteger photoIndex = 0;
    _actionSheetDicArray = [options objectForKey:@"actionSheet"];
    _name = [options objectForKey:KEY_NAME];
    _id = [[options objectForKey:KEY_ID] integerValue];
    _type = [options objectForKey:KEY_TYPE] ;
    NSArray *captions = [options objectForKey:@"captions"];
    _dateString = [options objectForKey:@"date"];
    if(_dateString == nil){
        _dateString = NSLocalizedString(@"Unknown Date",nil);
    }
    if(_name == nil){
        _name = NSLocalizedString(@"UNTITLED",nil);
    }
    
    for (NSString* url in imagesUrls)
    {
        [images addObject:[MWPhoto photoWithURL:[NSURL URLWithString: url]]];
    }
    if(captions != nil){
        if([captions count] == [images count] ){
            [images enumerateObjectsUsingBlock:^(MWPhoto*  _Nonnull photo, NSUInteger idx, BOOL * _Nonnull stop) {
                photo.caption = [captions objectAtIndex:idx];
            }];
        }
        
    }
    //#define DEBUG_CAPTION
#ifdef DEBUG_CAPTION
    else{
        NSArray *tempCaption = [NSArray arrayWithObjects:
                                @"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam in elit nullam.",
                                @"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec id bibendum justo, sed luctus lorem. Vestibulum euismod dolor in justo accumsan condimentum amet.",
                                @"Flat White at Elliot's",
                                @"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque auctor feugiat porttitor. In metus.",
                                @"Jury's Inn",
                                @"iPad Application Sketch Template v1",
                                @"Grotto of the Madonna", nil];
        [images enumerateObjectsUsingBlock:^(MWPhoto*  _Nonnull photo, NSUInteger idx, BOOL * _Nonnull stop) {
            int lowerBound = 0;
            int upperBound = (int)[tempCaption count] ;
            int rndValue = lowerBound + arc4random() % (upperBound - lowerBound);
            photo.caption = [tempCaption objectAtIndex:rndValue];
        }];
    }
#endif
    for (NSString* url in [options objectForKey:@"thumbnails"])
    {
        [thumbs addObject:[MWPhoto photoWithURL:[NSURL URLWithString: url]]];
    }
    _selections = [NSMutableArray new];
    for (int i = 0; i < images.count; i++) {
        [_selections addObject:[NSNumber numberWithBool:NO]];
    }
    self.photos = images;
    if([thumbs count] == 0){
        self.thumbs = self.photos;
    }else{
        self.thumbs = thumbs;
    }
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onOrientationChanged:) name:@"UIDeviceOrientationDidChangeNotification" object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onSDWebImageDownloadReceiveResponseNotification:) name:@"SDWebImageDownloadReceiveResponseNotification" object:nil];
    
    
    // Create & present browser
    MWPhotoBrowser *browser = [[MWPhotoBrowser alloc] initWithDelegate: self];
    _browser = browser;
    // Set options
    
    browser.displayActionButton = NO; // Show action button to save, copy or email photos (defaults to NO)
    browser.startOnGrid = YES;
    browser.enableGrid = YES;
    browser.displayNavArrows = NO;
    
    [browser setCurrentPhotoIndex: photoIndex]; // Example: allows second image to be presented first
    
    // Modal
    
    UINavigationController *nc = [[UINavigationController alloc] initWithRootViewController:browser];
    _navigationController = nc;
    
//    UIBarButtonItem *newAddBackButton = [[UIBarButtonItem alloc] initWithImage: OPTIONS_UIIMAGE style:UIBarButtonItemStylePlain target:self action:@selector(selectPhotos:)];
    UIBarButtonItem *newAddBackButton = [[UIBarButtonItem alloc] initWithTitle:NSLocalizedString(@"SELECT", nil) style:UIBarButtonItemStylePlain target:self action:@selector(selectPhotos:)];
    newAddBackButton.tag = 0;
    newAddBackButton.tintColor = LIGHT_BLUE_COLOR;
    
//    UIBarButtonItem *addAttachButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAdd target:self action:@selector(addPhotos:)];
//    addAttachButton.tintColor = LIGHT_BLUE_COLOR;
    browser.navigationController.navigationItem.rightBarButtonItems =  @[newAddBackButton];
    browser.navigationController.navigationItem.leftBarButtonItem.tintColor = LIGHT_BLUE_COLOR;
//    _addAttachButton = addAttachButton;
    _rightBarbuttonItem = newAddBackButton;
    
    _navigationController.delegate = self;
    
    CATransition *transition = [CATransition animation];
    transition.duration = VIEWCONTROLLER_TRANSITION_DURATION;
    
    transition.type = kCATransitionPush;
    transition.subtype = kCATransitionFromRight;
    [self.viewController.view.window.layer addAnimation:transition forKey:nil];
    [self.viewController presentViewController:nc animated:NO completion:^{
        
    }];
    
}
//-(void) selectAllPhotos:(UIBarButtonItem *)sender{
//    UIBarButtonItem *deselectAllButton = [[UIBarButtonItem alloc] initWithTitle: @"Deselect All" style:UIBarButtonItemStylePlain target:self action:@selector(deselectAllPhotos:)];
//    deselectAllButton.tag = SELECTALL_TAG;
//    _browser.navigationItem.leftBarButtonItem = deselectAllButton;
//
//    for (int i = 0; i < _selections.count; i++) {
//        [_selections replaceObjectAtIndex:i withObject:[NSNumber numberWithBool:YES]];
//    }
//    [_gridViewController.collectionView reloadData];
//
//}
//-(void) deselectAllPhotos:(UIBarButtonItem *)sender{
//    UIBarButtonItem *selectAllButton = [[UIBarButtonItem alloc] initWithTitle: @"Select All" style:UIBarButtonItemStylePlain target:self action:@selector(selectAllPhotos:)];
//    selectAllButton.tag = SELECTALL_TAG;
//    _browser.navigationItem.leftBarButtonItem = selectAllButton;
//    for (int i = 0; i < _selections.count; i++) {
//        [_selections replaceObjectAtIndex:i withObject:[NSNumber numberWithBool:NO]];
//    }
//    [_gridViewController.collectionView reloadData];
//}
-(void)selectPhotos:(UIBarButtonItem *)sender
{
    if(sender.tag == 0){
        dispatch_async(dispatch_get_main_queue(), ^{
            if(!_browser.displaySelectionButtons){
                _leftBarbuttonItem = _browser.navigationItem.leftBarButtonItem;
                _gridViewController.selectionMode = _browser.displaySelectionButtons = YES;
                [_gridViewController.collectionView reloadData];
                [_browser hideToolBar];
                //                sender.tag = 1;
                //                [sender setImage:nil];
                //                [sender setTitle:NSLocalizedString(@"Cancel", nil)];
                
                UIBarButtonItem * deleteBarButton = [[UIBarButtonItem alloc] initWithImage:BIN_UIIMAGE style:UIBarButtonItemStylePlain target:self action:@selector(deletePhotos:)];
                deleteBarButton.tintColor = LIGHT_BLUE_COLOR;
                _browser.navigationItem.rightBarButtonItems = @[deleteBarButton];
                UIBarButtonItem *closeButton = [[UIBarButtonItem alloc] initWithImage:CLOSE_UIIMAGE  style:UIBarButtonItemStylePlain target:self action:@selector(selectPhotos:)];
                closeButton.tag = 1;
                closeButton.tintColor = LIGHT_BLUE_COLOR;
                _browser.navigationItem.leftBarButtonItem = closeButton;
            }
        });
        
        //        PopupDialogDefaultView* dialogAppearance =  [PopupDialogDefaultView appearance];
        //        PopupDialogOverlayView* overlayAppearance =  [PopupDialogOverlayView appearance];
        //        overlayAppearance.blurEnabled = NO;
        //        overlayAppearance.blurRadius = 0;
        //        overlayAppearance.opacity = 0.5;
        //        dialogAppearance.titleTextAlignment     = NSTextAlignmentLeft;
        //        dialogAppearance.messageTextAlignment   = NSTextAlignmentLeft;
        //        dialogAppearance.titleFont              = [UIFont systemFontOfSize:20];
        //        dialogAppearance.messageFont            =  [UIFont systemFontOfSize:16];
        //        dialogAppearance.titleColor            =  [UIColor blackColor];
        //        dialogAppearance.messageColor            =  [UIColor darkGrayColor];
        //
        //        __weak PhotoBrowserPlugin *weakSelf = self;
        //        __block NSArray * titles =  [_actionSheetDicArray valueForKey:KEY_LABEL];
        //        __block NSArray * actions =  [_actionSheetDicArray valueForKey:KEY_ACTION];
        //
        //
        //        MKASOrientationConfig *portraitConfig = [[MKASOrientationConfig alloc] init];
        //        portraitConfig.titleAlignment = NSTextAlignmentLeft;
        //        portraitConfig.buttonTitleAlignment = MKActionSheetButtonTitleAlignment_left;
        //        portraitConfig.buttonHeight = 45.0f;
        //        portraitConfig.maxShowButtonCount = 5.5f;
        //
        //        MKASOrientationConfig *landscapeConfig = [[MKASOrientationConfig alloc] init];
        //        landscapeConfig.titleAlignment = NSTextAlignmentLeft;
        //        landscapeConfig.buttonTitleAlignment = MKActionSheetButtonTitleAlignment_left;
        //        landscapeConfig.buttonHeight = 30.0f;
        //        landscapeConfig.maxShowButtonCount = 2.5f;
        //
        //
        //        MKActionSheet *sheet = [[MKActionSheet alloc] initWithTitle:NSLocalizedString(@"Options", nil) buttonTitleArray:titles selectType:MKActionSheetSelectType_common];
        //        sheet.titleColor = [UIColor grayColor];
        //
        //        sheet.buttonTitleColor = [UIColor blackColor];
        //        sheet.buttonOpacity = 0.7;
        //
        //        sheet.animationDuration = 0.2f;
        //        sheet.blurOpacity = 0.7f;
        //        sheet.blackgroundOpacity = 0.6f;
        //        sheet.needCancelButton = NO;
        //
        //
        //        [sheet setPortraitConfig:portraitConfig];
        //        [sheet setLandscapeConfig:landscapeConfig];
        //
        //        [sheet showWithBlock:^(MKActionSheet *actionSheet, NSInteger buttonIndex) {
        //
        //            if([[actions objectAtIndex:buttonIndex] isEqualToString:DEFAULT_ACTION_ADD]){
        //                [self addPhotos:nil];
        //            }else if([[actions objectAtIndex:buttonIndex] isEqualToString:DEFAULT_ACTION_SELECT]){
        //                dispatch_async(dispatch_get_main_queue(), ^{
        //                    if(!_browser.displaySelectionButtons){
        //                        _leftBarbuttonItem = _browser.navigationItem.leftBarButtonItem;
        //                        _gridViewController.selectionMode = _browser.displaySelectionButtons = YES;
        //                        [_gridViewController.collectionView reloadData];
        //                        [_browser showToolBar];
        //                        sender.tag = 1;
        //                        [sender setImage:nil];
        //                        [sender setTitle:NSLocalizedString(@"Cancel", nil)];
        //                        _browser.navigationItem.rightBarButtonItems = @[_rightBarbuttonItem];
        //                    }
        //                });
        //            }
        //            else if([[actions objectAtIndex:buttonIndex] isEqualToString:DEFAULT_ACTION_RENAME]){
        //                //edit album name
        //                [weakSelf popupTextAreaDialogTitle:NSLocalizedString(@"Edit Album Name", nil) message:((_name != nil || [_name isEqualToString:@""] ) ? _name : NSLocalizedString(KEY_ALBUM, nil)) placeholder:NSLocalizedString(@"Album Name", nil) action:^(NSString * text) {
        //
        //                    //TODO send result edit album name
        //
        //                    if( ![text isEqualToString:@""]){
        //                        NSMutableDictionary *dictionary = [NSMutableDictionary new];
        //                        [dictionary setValue:[actions objectAtIndex:buttonIndex] forKey: KEY_ACTION];
        //                        [dictionary setValue:@(_id) forKey: KEY_ID];
        //                        [dictionary setValue:_type forKey: KEY_TYPE];
        //                        [dictionary setValue:text forKey: KEY_NAME];
        //                        [dictionary setValue:@"edit album name" forKey: @"description"];
        //                        _browser.navigationItem.titleView = [self setTitle:text subtitle:SUBTITLESTRING_FOR_TITLEVIEW(_dateString)];
        //                        _name = text;
        //                        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
        //                        [pluginResult setKeepCallbackAsBool:YES];
        //                        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
        //                    }
        //
        //                }];
        //            }else if([[actions objectAtIndex:buttonIndex] isEqualToString:DEFAULT_ACTION_DELETE]){
        //                [self buildDialogWithCancelText:NSLocalizedString(@"Cancel", nil) confirmText:NSLocalizedString(@"Delete", nil) title:
        //                 [NSString stringWithFormat:@"%@ %@", NSLocalizedString(@"Delete", nil), NSLocalizedString(_type, nil)]  text:NSLocalizedString(@"Are you sure you want to delete this album? This will also remove the Photos from the playlist if they are not in any other albums.", nil) action:^{
        //                     NSMutableDictionary *dictionary = [NSMutableDictionary new];
        //                     [dictionary setValue:[actions objectAtIndex:buttonIndex] forKey: KEY_ACTION];
        //                     [dictionary setValue:@(_id) forKey: KEY_ID];
        //                     [dictionary setValue:_type forKey: KEY_TYPE];
        //
        //                     [dictionary setValue:@"delete album" forKey: @"description"];
        //                     CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
        //                     [pluginResult setKeepCallbackAsBool:YES];
        //                     [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
        //                     [self photoBrowserDidFinishModalPresentation:_browser];
        //                 }];
        //
        //
        //            }else{
        //                NSMutableDictionary *dictionary = [NSMutableDictionary new];
        //                [dictionary setValue:[actions objectAtIndex:buttonIndex] forKey: KEY_ACTION];
        //                [dictionary setValue:@(_id) forKey: KEY_ID];
        //                [dictionary setValue:_type forKey: KEY_TYPE];
        //                [dictionary setValue:[actions objectAtIndex:buttonIndex] forKey: @"description"];
        //                CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
        //                [pluginResult setKeepCallbackAsBool:NO];
        //                [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
        //                [self photoBrowserDidFinishModalPresentation:_browser];
        //            }
        //
        //
        //        }];
        //
        //
        //        self.actionSheet = sheet;
    }else{
        _browser.navigationItem.leftBarButtonItem = _leftBarbuttonItem;
        _browser.navigationItem.rightBarButtonItems = @[_rightBarbuttonItem];
        if(_browser.displaySelectionButtons){
            _browser.displayActionButton = NO;
            _gridViewController.selectionMode = _browser.displaySelectionButtons = NO;
            [_gridViewController.collectionView reloadData];
            [_browser showToolBar];
            //            sender.tag = 0;
            //            [sender setImage:OPTIONS_UIIMAGE];
            //            [sender setTitle:nil];
            for (int i = 0; i < _selections.count; i++) {
                [_selections replaceObjectAtIndex:i withObject:[NSNumber numberWithBool:NO]];
            }
            
            
        }
        //        _browser.navigationItem.leftBarButtonItem = _leftBarbuttonItem;
        //        _browser.navigationItem.rightBarButtonItems = @[_rightBarbuttonItem, _addAttachButton];
        //add home back
    }
}

-(void) buildDialogWithCancelText:(NSString*)cancelText confirmText:(NSString*)confirmtext title:(NSString*) title text:(NSString*)text action:(void (^ _Nullable)(void))action {
    __weak PhotoBrowserPlugin *weakSelf = self;
    
    
    PopupDialog *popup = [[PopupDialog alloc] initWithTitle:title
                                                    message:text
                                                      image:nil
                                            buttonAlignment:UILayoutConstraintAxisHorizontal
                                            transitionStyle:PopupDialogTransitionStyleFadeIn
                                           gestureDismissal:YES
                                                 completion:nil];

    CancelButton *cancel = [[CancelButton alloc]initWithTitle:cancelText height:60 dismissOnTap:YES action:^{
        
    }];
    
    DefaultButton *ok = [[DefaultButton alloc]initWithTitle:confirmtext  height:60 dismissOnTap:YES action:action];
    [ok setBackgroundColor:LIGHT_BLUE_COLOR];
    [ok setTitleColor:[UIColor whiteColor]];
    [popup addButtons: @[cancel, ok]];
    _dialogView = popup;
    [_browser.navigationController presentViewController:popup animated:YES completion:nil];
    
}

- (void)popupTextAreaDialogTitle:(NSString*)title message:(NSString*)message placeholder:(NSString*)placeholder action:(void (^ _Nullable)(NSString*))action{
    
    
    __block TextInputViewController* textViewVC = [[TextInputViewController alloc] initWithNibName:@"TextInputViewController" bundle:nil];
    textViewVC.titleString = title;
    textViewVC.messageString = message;
    textViewVC.placeholderString = placeholder;
    
    __weak PhotoBrowserPlugin *weakSelf = self;
    PopupDialog *popup = [[PopupDialog alloc] initWithViewController:textViewVC buttonAlignment:UILayoutConstraintAxisHorizontal transitionStyle:PopupDialogTransitionStyleFadeIn gestureDismissal:YES completion:^{
        
    }];
    CancelButton *cancel = [[CancelButton alloc]initWithTitle:NSLocalizedString(@"CANCEL", nil) height:60 dismissOnTap:YES action:^{
        
    }];
    
    DefaultButton *ok = [[DefaultButton alloc]initWithTitle:NSLocalizedString(@"OK", nil)  height:60 dismissOnTap:YES action:^{
        action(textViewVC.textInputField.text);
    }];
    [ok setTitleColor:[UIColor whiteColor]];
    [ok setBackgroundColor:LIGHT_BLUE_COLOR];
    
    [popup addButtons: @[cancel, ok]];
    _dialogView = popup;
    [_browser.navigationController presentViewController:popup animated:YES completion:^{
        
    }];
}


#pragma mark UITextViewDelegate


-(void)textViewDidChange:(UITextView *)textView{
    MWPhoto *photo = [self.photos objectAtIndex:_browser.currentIndex];
    
    [photo setCaption:textView.text];
    [self.photos replaceObjectAtIndex:_browser.currentIndex withObject:photo];
    
}
- (BOOL)textViewShouldEndEditing:(UITextView *)textView{
    return YES;
}
-(void)textViewDidBeginEditing:(UITextView *)textView
{
    
    textView.backgroundColor = [UIColor whiteColor];
    textView.textColor = [UIColor blackColor];
}

- (void)textViewDidEndEditing:(UITextView *)textView
{
    textView.backgroundColor = [UIColor blackColor];
    textView.textColor = [UIColor whiteColor];
    [self resignKeyboard:textView];
    [self endEditCaption:textView];
    
}
- (BOOL)textViewShouldReturn:(UITextView *)textView{
    NSLog(@"textViewShouldReturn:");
    if (textView.tag == 1) {
        UITextView *textView = (UITextView *)[self.navigationController.view viewWithTag:2];
        [textView becomeFirstResponder];
    }
    else {
        [self endEditCaption:textView];
    }
    return YES;
}


- (BOOL)textView:(UITextView *)textView shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text {
    // Prevent crashing undo bug – see note below.
    IQTextView* iqTextView = (IQTextView*)textView;
    iqTextView.shouldHidePlaceholderText = NO;
    iqTextView.placeholderText = [NSString stringWithFormat:@"%lu/%d",(unsigned long)textView.text.length, MAX_CHARACTER];
    
    if(range.length + range.location > textView.text.length)
    {
        return NO;
    }
    if ([text isEqualToString:@"\n"]) {
        [textView resignFirstResponder];
        
        return NO;
    }
    NSUInteger newLength = [textView.text length] + [text length] - range.length;
    return newLength < MAX_CHARACTER;
}

#pragma mark - MWPhotoBrowserDelegate

- (NSUInteger)numberOfPhotosInPhotoBrowser:(MWPhotoBrowser *)photoBrowser {
    return _photos.count;
}

- (MWPhoto *)photoBrowser:(MWPhotoBrowser *)photoBrowser photoAtIndex:(NSUInteger)index {
    if (index < _photos.count)
    return [_photos objectAtIndex:index];
    return nil;
}
- (id <MWPhoto>)photoBrowser:(MWPhotoBrowser *)photoBrowser thumbPhotoAtIndex:(NSUInteger)index{
    MWPhoto *photo = [self.thumbs objectAtIndex:index];
    return photo;
}
- (MWCaptionView *)photoBrowser:(MWPhotoBrowser *)photoBrowser captionViewForPhotoAtIndex:(NSUInteger)index {
    MWPhoto *photo = [self.photos objectAtIndex:index];
    MWCaptionView *captionView = [[MWCaptionView alloc] initWithPhoto:photo];
    captionView.backgroundColor = [UIColor clearColor];
    return captionView;
}

-(void) photoBrowserDidFinishModalPresentation:(MWPhotoBrowser*) browser{
    CATransition *transition = [CATransition animation];
    
    transition.duration = VIEWCONTROLLER_TRANSITION_DURATION;
    transition.delegate = self;
    transition.type = kCATransitionPush;
    transition.subtype = kCATransitionFromLeft;
    [browser.view.window.layer addAnimation:transition forKey:nil];
    [browser dismissViewControllerAnimated:NO completion:^{
        
        
    }];
}

- (void)animationDidStop:(CAAnimation *)anim finished:(BOOL)flag{
    if(flag){
        _photos = nil;
        _thumbs = nil;
        _data = nil;
        _navigationController = nil;
        _gridViewController = nil;
        _browser = nil;
        _actionSheet = nil;
        _name = nil;
        _dialogView = nil;
        _rightBarbuttonItem = nil;
        _addAttachButton = nil;
    }
}
- (void)photoBrowser:(MWPhotoBrowser *)photoBrowser didDisplayPhotoAtIndex:(NSUInteger)index{
    _browser = photoBrowser;
    NSLog(@"didDisplayPhotoAtIndex %lu", (unsigned long)index);
    if(_textView.superview != nil){
        _textView.text = [[self.photos objectAtIndex:index] caption];
        [_textView setFrame:[self newRectFromTextView:_textView ]];
    }
    
}
- (void)photoBrowser:(MWPhotoBrowser *)photoBrowser actionButtonPressedForPhotoAtIndex:(NSUInteger)index{
    _browser = photoBrowser;
    NSLog(@"actionButtonPressedForPhotoAtIndex %lu", (unsigned long)index);
}
- (BOOL)photoBrowser:(MWPhotoBrowser *)photoBrowser isPhotoSelectedAtIndex:(NSUInteger)index{
    _browser = photoBrowser;
    return [[_selections objectAtIndex:index] boolValue];
}
- (void)photoBrowser:(MWPhotoBrowser *)photoBrowser photoAtIndex:(NSUInteger)index selectedChanged:(BOOL)selected{
    _browser = photoBrowser;
    [_selections replaceObjectAtIndex:index withObject:[NSNumber numberWithBool:selected]];
    NSLog(@"photoAtIndex %lu selectedChanged %i", (unsigned long)index , selected);
}
- (BOOL)photoBrowser:(MWPhotoBrowser *)photoBrowser showGridController:(MWGridViewController*)gridController{
//    [photoBrowser hideToolBar];
    _browser = photoBrowser;
    _gridViewController = gridController;
    if(_rightBarbuttonItem != nil){
        
        //        UIBarButtonItem *addAttachButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemAdd target:self action:@selector(addPhotos:)];
        photoBrowser.navigationItem.rightBarButtonItems = @[_rightBarbuttonItem];
        [_rightBarbuttonItem setAction:@selector(selectPhotos:)];
        [_rightBarbuttonItem setTarget:self];
        [_browser showToolBar];
    }
    if(_textView != nil){
        [self resignKeyboard:_textView];
        [self endEditCaption:_textView];
    }
//    [_browser hideToolBar];
    [_browser showToolBar];
    return YES;
}
- (void) addPhotos:(id) sender{
    NSMutableDictionary *dictionary = [NSMutableDictionary new];
    [dictionary setValue:DEFAULT_ACTION_ADD forKey: KEY_ACTION];
    [dictionary setValue:@(_id) forKey: KEY_ID];
    [dictionary setValue:_type forKey: KEY_TYPE];
    [dictionary setValue:@"add photo to album" forKey: @"description"];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
    [pluginResult setKeepCallbackAsBool:NO];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
    [self photoBrowserDidFinishModalPresentation:_browser];
}
- (BOOL)photoBrowser:(MWPhotoBrowser *)photoBrowser hideGridController:(MWGridViewController*)gridController{
    _browser = photoBrowser;
    _gridViewController = nil;
    
    if(_textView != nil){
        [_textView removeFromSuperview];
    }
    photoBrowser.navigationItem.rightBarButtonItems = nil;
    photoBrowser.navigationController.navigationItem.rightBarButtonItems = nil;
    [photoBrowser showToolBar];
    return YES;
}

- (BOOL)photoBrowser:(MWPhotoBrowser *)photoBrowser setNavBarAppearance:(UINavigationBar *)navigationBar{
    
    _browser = photoBrowser;
    [photoBrowser.navigationController setNavigationBarHidden:NO animated:NO];
    navigationBar.barStyle = UIBarStyleDefault;
    //    navigationBar.translucent = YES;
    navigationBar.barTintColor = [UIColor whiteColor];
    photoBrowser.navigationItem.titleView = [self setTitle:_name subtitle:SUBTITLESTRING_FOR_TITLEVIEW(_dateString)];
    return YES;
}

-(UIView*) setTitle:(NSString*)title subtitle:(NSString*)subtitle {
    UILabel *titleLabel = [[UILabel alloc] initWithFrame: CGRectMake(0,-5,0,18)];
    
    titleLabel.backgroundColor = [UIColor clearColor];
    titleLabel.textColor = [UIColor blackColor];
    titleLabel.font = [UIFont boldSystemFontOfSize: 17];
    titleLabel.text = title;
    titleLabel.numberOfLines = 1;
    titleLabel.minimumScaleFactor = 0.8f;
    [titleLabel sizeToFit];
    
    UILabel *subtitleLabel = [[UILabel alloc] initWithFrame: CGRectMake(0,18,0,0)];
    subtitleLabel.backgroundColor = [UIColor clearColor];
    subtitleLabel.textColor = [UIColor blackColor];
    subtitleLabel.font = [UIFont systemFontOfSize:12];
    subtitleLabel.text = subtitle;
    [subtitleLabel sizeToFit];
    
    
    
    UIView *titleView = [[UIView alloc] initWithFrame:CGRectMake(0, 0, fmax(titleLabel.frame.size.width, subtitleLabel.frame.size.width), 30)];
    [titleView addSubview:titleLabel];
    [titleView addSubview:subtitleLabel];
    
    float widthDiff = subtitleLabel.frame.size.width - titleLabel.frame.size.width;
    
    if (widthDiff > 0) {
        CGRect frame = titleLabel.frame;
        frame.origin.x = widthDiff / 2;
        titleLabel.frame = CGRectIntegral(frame);
    } else {
        CGRect frame = subtitleLabel.frame;
        frame.origin.x = fabsf(widthDiff) / 2;
        subtitleLabel.frame = CGRectIntegral(frame);
    }
    
    return titleView;
}

-(BOOL) photoBrowserSelectionMode{
    return _browser.displaySelectionButtons;
}
- (BOOL)photoBrowser:(MWPhotoBrowser *)photoBrowser hideToolbar:(BOOL)hide {
    _browser = photoBrowser;
    return NO;
}
- (NSMutableArray*)photoBrowser:(MWPhotoBrowser *)photoBrowser buildToolbarItems:(UIToolbar*)toolBar{
    _toolBar = toolBar;
    if(_gridViewController != nil){
        NSMutableArray *items = [[NSMutableArray alloc] init];
        
        if(_browser.displaySelectionButtons){
            
        }else{
//            UIBarButtonItem *fixedSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:self action:nil];
//            fixedSpace.width = 32; // To balance action button
            UIBarButtonItem *flexSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:self action:nil];
            [items addObject:flexSpace];
            float margin = 2;
            CGRect newFrame = CGRectMake(toolBar.frame.origin.x - margin, toolBar.frame.origin.y - margin, toolBar.frame.size.width - margin*2, toolBar.frame.size.height - margin*2 );
            UIButton *btn = [[UIButton alloc] initWithFrame: newFrame];
        
            //    [button setImage:[UIImage imageNamed:[NSString stringWithFormat:@"%@.bundle/%@", NSStringFromClass([self class]), @"images/send.png"]] forState:UIControlStateNormal];
            [btn setBackgroundColor:LIGHT_BLUE_COLOR];
            btn.layer.cornerRadius = 10; // this value vary as per your desire
            btn.clipsToBounds = YES;
            [btn setTitle:NSLocalizedString(@"ADD_PHOTOS", nil) forState:UIControlStateNormal];
            [btn addTarget:self action:@selector(addPhotos:) forControlEvents:UIControlEventTouchUpInside];
            UIBarButtonItem *addPhotoButton = [[UIBarButtonItem alloc] initWithCustomView:btn];
            
            //            UIBarButtonItem * addPhotoButton = [[UIBarButtonItem alloc] initWithTitle:NSLocalizedString(@"ADD_PHOTOS", nil) style:UIBarButtonItemStylePlain target:self action:@selector(m:)];
            
            [items addObject:addPhotoButton];

//            UIBarButtonItem * sendtoBarButton = [[UIBarButtonItem alloc] initWithImage:SEND_UIIMAGE style:UIBarButtonItemStylePlain target:self action:@selector(sendTo:)];
//            sendtoBarButton.tintColor = LIGHT_BLUE_COLOR;
//            [items addObject:sendtoBarButton];
            [items addObject:flexSpace];
            
//            _toolBar.translucent = NO;
            _toolBar.barStyle = UIBarStyleDefault;
//            _toolBar.tintColor = [UIColor whiteColor];
            _toolBar.barTintColor = [UIColor whiteColor];;
        }
//
//        
//        UIBarButtonItem * deleteBarButton = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemTrash
//                                                                                          target:self action:@selector(deletePhotos:)];
//        
//        //        UIBarButtonItem *selectAllButton = [[UIBarButtonItem alloc] initWithTitle: @"SELECT_ALL" style:UIBarButtonItemStylePlain target:self action:@selector(selectAllPhotos:)];
//        //        selectAllButton  .tag = SELECTALL_TAG;
//        //        photoBrowser.navigationItem.leftBarButtonItem = selectAllButton;
//        
//        
//        [items addObject:deleteBarButton];
//        if(IS_TYPE_ALBUM){
//            [items addObject:flexSpace];
//            UIBarButtonItem * downloadPhotosButton = [[UIBarButtonItem alloc] initWithImage:DOWNLOADIMAGE_UIIMAGE style:UIBarButtonItemStylePlain target:self action:@selector(downloadPhotos:)];
//            [items addObject:downloadPhotosButton];
//            [items addObject:flexSpace];
//            UIBarButtonItem * sendtoBarButton = [[UIBarButtonItem alloc] initWithImage:SEND_UIIMAGE style:UIBarButtonItemStylePlain target:self action:@selector(sendTo:)];
//            [items addObject:sendtoBarButton];
//            
//        }
//        //TODO add Select All at left
//        
        return items;
    }else{
        UIBarButtonItem *fixedSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFixedSpace target:self action:nil];
        fixedSpace.width = 32; // To balance action button
        UIBarButtonItem *flexSpace = [[UIBarButtonItem alloc] initWithBarButtonSystemItem:UIBarButtonSystemItemFlexibleSpace target:self action:nil];
        NSMutableArray *items = [[NSMutableArray alloc] init];
        if(IS_TYPE_ALBUM){
            UIBarButtonItem * downloadPhotoButton = [[UIBarButtonItem alloc] initWithImage:DOWNLOADIMAGE_UIIMAGE style:UIBarButtonItemStylePlain target:self action:@selector(downloadPhoto:)];
            
            UIBarButtonItem * editCaption = [[UIBarButtonItem alloc] initWithImage:EDIT_UIIMAGE style:UIBarButtonItemStylePlain target:self action:@selector(beginEditCaption:)];
            [items addObject:downloadPhotoButton];
            [items addObject:flexSpace];
            [items addObject:editCaption];
            [items addObject:flexSpace];
        }
        UIBarButtonItem * deleteBarButton = [[UIBarButtonItem alloc] initWithImage:BIN_UIIMAGE style:UIBarButtonItemStylePlain target:self action:@selector(deletePhotos:)];
        [items addObject:deleteBarButton];
        _toolBar.translucent = NO;
        _toolBar.barStyle = UIBarStyleDefault;
        _toolBar.tintColor = LIGHT_BLUE_COLOR;
//        _toolBar.barTintColor = [UIColor whiteColor];
        return items;
    }
    
    
}
-(void)onOrientationChanged:(id)orientation{
    _browser.navigationItem.titleView = [self setTitle:_name subtitle:SUBTITLESTRING_FOR_TITLEVIEW(_dateString)];
}

-(void) downloadPhoto:(id)sender{
    //TODO save photo
    __block MBProgressHUD *progressHUD = [MBProgressHUD showHUDAddedTo:_browser.view
                                                              animated:YES];
    progressHUD.mode = MBProgressHUDModeDeterminate;
    
    progressHUD.label.text = NSLocalizedString(@"DOWNLOADING",nil);
    [progressHUD showAnimated:YES];
    
    @try{
        NSString *originalUrl = [[_data objectAtIndex:_browser.currentIndex] objectForKey:@"originalUrl"];
        if(originalUrl != nil){
            [[SDWebImageManager sharedManager] loadImageWithURL:[NSURL URLWithString:originalUrl] options:0 progress:^(NSInteger receivedSize, NSInteger expectedSize, NSURL * _Nullable targetURL) {
                [progressHUD setProgress:(receivedSize*1.0f)/(expectedSize*1.0f) ];
            } completed:^(UIImage * _Nullable image, NSData * _Nullable data, NSError * _Nullable error, SDImageCacheType cacheType, BOOL finished, NSURL * _Nullable imageURL) {
                
                if ([PHObject class]) {
                    __block PHAssetChangeRequest *assetRequest;
                    __block PHObjectPlaceholder *placeholder;
                    // Save to the album
                    [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
                        
                        [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
                            assetRequest = [PHAssetChangeRequest creationRequestForAssetFromImage:image];
                            placeholder = [assetRequest placeholderForCreatedAsset];
                        } completionHandler:^(BOOL success, NSError *error) {
                            
                            dispatch_async(dispatch_get_main_queue(), ^{
                                NSString *message;
                                NSString *title;
                                [progressHUD hideAnimated:YES];
                                if (success) {
                                    title = NSLocalizedString(@"Image Saved", @"");
                                    message = NSLocalizedString(@"The image was placed in your photo album.", @"");
                                }
                                else {
                                    title = NSLocalizedString(@"Error", @"");
                                    message = [error description];
                                }
                                UIAlertView *alert = [[UIAlertView alloc] initWithTitle:title
                                                                                message:message
                                                                               delegate:nil
                                                                      cancelButtonTitle:@"OK"
                                                                      otherButtonTitles:nil];
                                [alert show];
                            });
                            
                        }];
                    }];
                }
                
                
            }];
        }else{
            NSString *message;
            NSString *title;
            [progressHUD hideAnimated:YES];
            
            title = NSLocalizedString(@"Error", @"");
            message =  NSLocalizedString(@"Photo is not available", @"");
            
            UIAlertView *alert = [[UIAlertView alloc] initWithTitle:title
                                                            message:message
                                                           delegate:nil
                                                  cancelButtonTitle:@"OK"
                                                  otherButtonTitles:nil];
            [alert show];
        }
        //download
    }@catch(NSException * exception){
        NSLog(@"%@", exception.description);
    }
}

typedef void(^DownloaderProgressBlock)(float progress);

typedef void(^DownloaderCompletedBlock)(NSArray *images, NSError *error, BOOL finished);


-(void)downloadPhotos:(id)sender{
    NSMutableArray* urls = [NSMutableArray new];
    [_selections enumerateObjectsUsingBlock:^(NSNumber *obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if([obj boolValue]){
            NSString *originalUrl = [[_data objectAtIndex:idx] objectForKey:@"originalUrl"];
            if(originalUrl != nil){
                [urls addObject:originalUrl];
            }
        }
    }];
    if([urls count] > 0 ){
        __block MBProgressHUD *progressHUD = [MBProgressHUD showHUDAddedTo:_browser.view
                                                                  animated:YES];
        progressHUD.mode = MBProgressHUDModeDeterminate;
        
        progressHUD.label.text = NSLocalizedString(@"Downloading",nil);
        [progressHUD showAnimated:YES];
        
        
        [self downloadImages:urls total:[urls count] received:0 progress:^(float progress) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [progressHUD setProgress:progress];
            });
        } complete:^(NSArray *images, NSError *error, BOOL finished) {
            dispatch_async(dispatch_get_main_queue(), ^{
                [progressHUD hideAnimated:YES];
                NSString *message;
                NSString *title;
                
                if (error == nil) {
                    title = NSLocalizedString(@"Images Saved", @"");
                    message = NSLocalizedString(@"The image was placed in your photo album.", @"");
                }
                else {
                    title = NSLocalizedString(@"Error", @"");
                    message = [error description];
                }
                UIAlertView *alert = [[UIAlertView alloc] initWithTitle:title
                                                                message:message
                                                               delegate:nil
                                                      cancelButtonTitle:@"OK"
                                                      otherButtonTitles:nil];
                [alert show];
            });
            
            
        } ];
    }
    
}

-(void) downloadImages:(NSArray*)urls total:(NSInteger)total received:(NSInteger)received progress:(DownloaderProgressBlock) progressBlack complete:(DownloaderCompletedBlock)completeBlock{
    SDWebImageManager *manager = [SDWebImageManager sharedManager];
    [manager loadImageWithURL:[NSURL URLWithString:[urls firstObject]] options:0 progress:^(NSInteger receivedSize, NSInteger expectedSize, NSURL * _Nullable targetURL) {
        float progressOfATask = ((receivedSize*1.0f)/(expectedSize*1.0f))*(1.0f/total*1.0f);
        progressBlack(((received*1.0f)/(total*1.0f))+progressOfATask);
        
    } completed:^(UIImage * _Nullable image, NSData * _Nullable data, NSError * _Nullable error, SDImageCacheType cacheType, BOOL finished, NSURL * _Nullable imageURL) {
        if ([PHObject class]) {
            __block PHAssetChangeRequest *assetRequest;
            __block PHObjectPlaceholder *placeholder;
            [PHPhotoLibrary requestAuthorization:^(PHAuthorizationStatus status) {
                
                [[PHPhotoLibrary sharedPhotoLibrary] performChanges:^{
                    assetRequest = [PHAssetChangeRequest creationRequestForAssetFromImage:image];
                    placeholder = [assetRequest placeholderForCreatedAsset];
                } completionHandler:^(BOOL success, NSError *error) {
                    if (success) {
                        if([urls count] > 1){
                            NSArray *tempArray = [NSArray arrayWithArray:[urls subarrayWithRange: NSMakeRange (1, [urls count]-1) ]];
                            NSInteger newReceive = (received+1);
                            [self downloadImages:tempArray total:total received:newReceive progress:progressBlack complete:completeBlock];
                        }else{
                            completeBlock(nil, nil, YES);
                        }
                    }
                    else {
                        NSError* err = [NSError errorWithDomain:@"PhotoBrowserPlugin" code:403 userInfo:@{NSLocalizedDescriptionKey:@"Photo Library is not allowed to access"} ];
                        completeBlock(nil, err, YES);
                    }
                }];
            }];
        }
    }];
}

- (NSString*)tempFilePath:(NSString*)extension
{
    NSString* docsPath = [NSTemporaryDirectory()stringByStandardizingPath];
    NSFileManager* fileMgr = [[NSFileManager alloc] init]; // recommended by Apple (vs [NSFileManager defaultManager]) to be threadsafe
    NSString* filePath;
    
    // generate unique file name
    int i = 1;
    do {
        filePath = [NSString stringWithFormat:@"%@/%@%03d.%@", docsPath, CDV_PHOTO_PREFIX, i++, extension];
    } while ([fileMgr fileExistsAtPath:filePath]);
    
    return filePath;
}


-(void) beginEditCaption:(UIBarButtonItem*)sender{
    
    if(_browser != nil){
        _browser.alwaysShowControls = YES;
    }
    if(_textView == nil){
        float height = self.navigationController.view.frame.size.height*(1.0f/6.0f);
        float y = self.navigationController.view.frame.size.height - height ;
        
        _textView = [[IQTextView alloc ] initWithFrame:CGRectMake(0, y, self.navigationController.view.frame.size.width, height*.5)];
        _textView.delegate = self;
        _textView.backgroundColor = [UIColor blackColor];
        _textView.textColor = [UIColor whiteColor];
        _textView.font = [UIFont systemFontOfSize:17];
        _textView.returnKeyType = UIReturnKeyDone;
        [_textView addRightButtonOnKeyboardWithImage:EDIT_UIIMAGE target:self action:@selector(resignKeyboard:) shouldShowPlaceholder:nil];
        [[IQKeyboardManager sharedManager] preventShowingBottomBlankSpace];
    }
    __block MWPhoto *photo = [self.photos objectAtIndex:[_browser currentIndex]];
    
    _textView.text = photo.caption;
    
    _textView.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleLeftMargin | UIViewAutoresizingFlexibleRightMargin | UIViewAutoresizingFlexibleTopMargin;
    [_textView setFrame:[self newRectFromTextView:_textView ]];
    [_browser.view addSubview:_textView];
    [_textView becomeFirstResponder];
}
-(void) resignKeyboard:(id)sender{
    if(_textView && _textView.superview != nil){
        [_textView resignFirstResponder];
        [_textView removeFromSuperview];
    }
}
-(void) endEditCaption:(id)sender{
    _browser.alwaysShowControls = NO;
    [[self.photos objectAtIndex:_browser.currentIndex] setCaption: _textView.text];
    
    [_browser reloadData];
    [[IQKeyboardManager sharedManager] setKeyboardDistanceFromTextField:0];
    NSMutableDictionary *dictionary = [NSMutableDictionary new];
    [dictionary setValue:[_data objectAtIndex:_browser.currentIndex] forKey: @"photo"];
    [dictionary setValue:[[_photos objectAtIndex:_browser.currentIndex] caption] forKey: @"caption"];
    [dictionary setValue:@"editCaption" forKey: KEY_ACTION];
    [dictionary setValue:@(_id) forKey: KEY_ID];
    [dictionary setValue:_type forKey: KEY_TYPE];
    [dictionary setValue:@"edit caption of photo" forKey: @"description"];
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
}
-(CGRect) newRectFromTextView:(UITextView*) inTextView{
    float labelPadding = 10;
    float newHeight =  MAX(MIN(5.0,(_textView.contentSize.height - _textView.textContainerInset.top - _textView.textContainerInset.bottom) / _textView.font.lineHeight), 2) *_textView.font.lineHeight ;
    newHeight = MAX(newHeight , _toolBar.frame.size.height)  + labelPadding * 2;
    CGRect originFrame = _textView.frame;
    CGRect newFrame = CGRectMake( originFrame.origin.x, self.navigationController.view.frame.size.height - newHeight - _toolBar.frame.size.height, originFrame.size.width, newHeight);
    return newFrame;
}
-(void) deletePhoto:(id)sender{
    [self buildDialogWithCancelText:NSLocalizedString(@"Cancel", nil) confirmText:NSLocalizedString(@"Delete", nil) title:NSLocalizedString(@"Delete Photos", nil) text:NSLocalizedString(@"Are you sure you want to delete the selected photos?", nil) action:^{
        NSMutableArray* tempPhotos = [NSMutableArray arrayWithArray:_photos];
        NSMutableArray* tempThumbs = [NSMutableArray arrayWithArray:_thumbs];
        NSMutableArray* tempSelections = [NSMutableArray arrayWithArray:_selections];
        NSDictionary* targetPhoto = [_data objectAtIndex:_browser.currentIndex];
        [tempPhotos removeObjectAtIndex:_browser.currentIndex];
        [tempThumbs removeObjectAtIndex:_browser.currentIndex];
        [tempSelections removeObjectAtIndex:_browser.currentIndex];
        self.photos = tempPhotos;
        self.thumbs = tempThumbs;
        _selections = tempSelections;
        if([targetPhoto valueForKey:KEY_ID] != nil){
            _browser.navigationItem.titleView = [self setTitle:_name subtitle:SUBTITLESTRING_FOR_TITLEVIEW(_dateString)];
            NSMutableDictionary *dictionary = [NSMutableDictionary new];
            [dictionary setValue:@[[targetPhoto valueForKey:KEY_ID]] forKey: KEY_PHOTOS];
            [dictionary setValue:KEY_DELETEPHOTOS forKey: KEY_ACTION];
            [dictionary setValue:@(_id) forKey: KEY_ID];
            [dictionary setValue:_type forKey: KEY_TYPE];
            [dictionary setValue:@"delete photo" forKey: @"description"];
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
            [pluginResult setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
            if([_photos count] == 0){
                [self photoBrowserDidFinishModalPresentation:_browser];
            }else{
                [_browser reloadData];
            }
        }
    }];
    
}
-(void) deletePhotos:(id)sender{
    
    __block NSMutableArray *fetchArray = [NSMutableArray new];
    __block NSMutableArray* tempPhotos = [NSMutableArray new];
    __block NSMutableArray* tempThumbs = [NSMutableArray new];
    __block NSMutableArray* tempSelections = [NSMutableArray new];
    __block NSMutableArray* tempData = [NSMutableArray new];
    
    [_selections enumerateObjectsUsingBlock:^(NSNumber *obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if([obj boolValue]){
            NSDictionary* object = [_data objectAtIndex:idx];
            if([object objectForKey:KEY_ID] != nil){
                [fetchArray addObject: [object objectForKey:KEY_ID]];
            }
            
        }else{
            [tempPhotos addObject: [_photos objectAtIndex:idx]];
            [tempThumbs addObject: [_thumbs objectAtIndex:idx]];
            [tempSelections addObject: [_selections objectAtIndex:idx]];
            [tempData addObject: [_data objectAtIndex:idx]];
        }
    }];
    if([fetchArray count] > 0 ){
        [self buildDialogWithCancelText:NSLocalizedString(@"Cancel", nil) confirmText:NSLocalizedString(@"Delete", nil) title:NSLocalizedString(@"Delete Photos", nil) text:NSLocalizedString(@"Are you sure you want to delete the selected photos?", nil) action:^{
            
            
            self.photos = tempPhotos;
            self.thumbs = tempThumbs;
            _selections = tempSelections;
            _data = tempData;
            if([_photos count]>1){
                [_browser setCurrentPhotoIndex:0];
            }
            
            _browser.navigationItem.titleView = [self setTitle:_name subtitle:SUBTITLESTRING_FOR_TITLEVIEW(_dateString)];
            NSMutableDictionary *dictionary = [NSMutableDictionary new];
            [dictionary setValue:fetchArray forKey: KEY_PHOTOS];
            [dictionary setValue:KEY_DELETEPHOTOS forKey: KEY_ACTION];
            [dictionary setValue:@(_id) forKey: KEY_ID];
            [dictionary setValue:_type forKey: KEY_TYPE];
            [dictionary setValue:@"delete photos from album" forKey: @"description"];
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
            [pluginResult setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
            if([_photos count] == 0){
                [self photoBrowserDidFinishModalPresentation:_browser];
            }else{
                [_browser reloadData];
            }
        }];
    }
    
}
-(void) sendTo:(id)sender{
    
    __block NSMutableArray *fetchArray = [NSMutableArray new];
    [_selections enumerateObjectsUsingBlock:^(NSNumber *obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if([obj boolValue]){
            NSDictionary* object = [_data objectAtIndex:idx];
            if([object objectForKey:KEY_ID] != nil){
                [fetchArray addObject: [object objectForKey:KEY_ID]];
            }
            
        }
    }];
    if([fetchArray count] > 0 ){
        NSMutableDictionary *dictionary = [NSMutableDictionary new];
        [dictionary setValue:fetchArray forKey: KEY_PHOTOS];
        [dictionary setValue:@"send" forKey: KEY_ACTION];
        [dictionary setValue:@(_id) forKey: KEY_ID];
        [dictionary setValue:_type forKey: KEY_TYPE];
        [dictionary setValue:@"send photos to destination" forKey: @"description"];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:dictionary];
        [pluginResult setKeepCallbackAsBool:NO];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:self.callbackId];
        [self photoBrowserDidFinishModalPresentation:_browser];
    }
    
}
-(void) actionButtonPressed:(id)sender{
    
}


-(void)onSDWebImageDownloadReceiveResponseNotification:(NSNotification*)notification{
    
    SDWebImageDownloaderOperation *operation = ((SDWebImageDownloaderOperation *)[notification valueForKey:@"object"]);
    NSHTTPURLResponse* response = ((NSHTTPURLResponse*)operation.response);
    NSURL *url = [response URL];
    NSString* key = @"x-amz-meta-orientation";
    if([[response allHeaderFields] objectForKey:key]){
        NSString *value = [[response allHeaderFields] valueForKey:key];
        [_HTTPResponseHeaderOrientations setValue:@([value integerValue]) forKey:[url absoluteString]];
    }
}

static inline double radians (double degrees) {return degrees * M_PI/180;}
- (UIImage *)imageManager:(SDWebImageManager *)imageManager transformDownloadedImage:(UIImage *)image withURL:(NSURL *)imageURL{
    NSString* key = [imageURL absoluteString];
    if([_HTTPResponseHeaderOrientations objectForKey:key]){
        NSNumber *value = [_HTTPResponseHeaderOrientations valueForKey:key];
        UIImage* retImage = rotate(image, ((enum Orientation)[value integerValue]));
        [_HTTPResponseHeaderOrientations removeObjectForKey:key];
        return retImage;
    }
    return image;
    
}

UIImage* rotate(UIImage* src, enum Orientation orientation)
{
    double rotation = 0;
    switch (orientation) {
        case RIGHT_BOTTOM:
        rotation = radians(-90);
        break;
        case BOTTOM_LEFT:
        rotation = radians(180);
        break;
        case RIGHT_TOP:
        rotation = radians(90);
        break;
        default :
        rotation = 0;
        break;
    }
    
    CGAffineTransform t = CGAffineTransformMakeRotation(rotation);
    CGRect sizeRect = CGRectMake(0, 0, src.size.width, src.size.height);
    CGRect destRect = CGRectApplyAffineTransform(sizeRect, t);
    CGSize destinationSize = destRect.size;
    
    // Draw image
    UIGraphicsBeginImageContext(destinationSize);
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextTranslateCTM(context, destinationSize.width / 2.0f, destinationSize.height / 2.0f);
    CGContextRotateCTM(context, rotation);
    [src drawInRect:CGRectMake(-src.size.width / 2.0f, -src.size.height / 2.0f, src.size.width, src.size.height)];
    
    // Save image
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return newImage;
}
@end
