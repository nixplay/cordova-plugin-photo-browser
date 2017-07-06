//
//  AppDelegate_LockOrientation.h
//  Nixplay
//
//  Created by James Kong on 6/7/2017.
//
//

#import "AppDelegate.h"
@implementation AppDelegate (LockOrientation)

- (UIInterfaceOrientationMask)application:(UIApplication *)application supportedInterfaceOrientationsForWindow:(UIWindow *)window{
    return UIInterfaceOrientationMaskPortrait;
}
@end
