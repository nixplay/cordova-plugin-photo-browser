//
//  TextInputViewController.m
//  Nixplay
//
//  Created by James Kong on 4/5/2017.
//
//

#import "TextInputViewController.h"

@interface TextInputViewController ()
@end

@implementation TextInputViewController
@synthesize titleLabel = _titleLabel;
@synthesize textInputField = _textInputField;
@synthesize titleString = _titleString;
@synthesize messageString = _messageString;
@synthesize placeholderString = _placeholderString;
- (void)viewDidLoad {
    [super viewDidLoad];
    if(_titleString == nil || [_titleString isEqualToString:@""]){
        _titleString = @"Default Title";
    }
    if(_messageString == nil || [_messageString isEqualToString:@""]){
        _messageString = @"Default Message";
    }
    if(_placeholderString == nil || [_placeholderString isEqualToString:@""]){
        _placeholderString = @"Default PlaceHolder";
    }
    [_titleLabel setText:_titleString];
    [_textInputField setText:_messageString];
    [_textInputField setPlaceholder:_placeholderString];
    
    // Do any additional setup after loading the view from its nib.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/*
 #pragma mark - Navigation
 
 // In a storyboard-based application, you will often want to do a little preparation before navigation
 - (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
 // Get the new view controller using [segue destinationViewController].
 // Pass the selected object to the new view controller.
 }
 */

@end
