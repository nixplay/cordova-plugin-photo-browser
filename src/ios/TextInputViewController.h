//
//  TextInputViewController.h
//  Nixplay
//
//  Created by James Kong on 4/5/2017.
//
//

#import <UIKit/UIKit.h>

@interface TextInputViewController : UIViewController

@property (weak, nonatomic) IBOutlet UILabel *titleLabel;
@property (weak, nonatomic) IBOutlet UITextField *textInputField;
@property (nonatomic , retain) NSString *titleString;
@property (nonatomic , retain) NSString *messageString;
@property (nonatomic , retain) NSString *placeholderString;
@end
