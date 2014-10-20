//
//  RadarWaveView.h
//  Carma
//
//  Created by Li Jiantang on 10/10/2014.
//
//

#import <UIKit/UIKit.h>

typedef struct AnimationPace {
    float pace;
    float scale;
    float alpha;
    BOOL active;
} AnimationPace;

@interface RadarWaveView : UIView {
    AnimationPace *animationPaces;
    int animationPacesLength;
    
    float alphaPaceThreshold;
    float alphaMaximumPace;
    UIColor *strokeColor;
    
    BOOL staticMode;
    
    BOOL debug;
}

@property (nonatomic) float maximumPace;
@property (nonatomic) float alphaPaceThreshold;
@property (nonatomic) float alphaMaximumPace;
@property (nonatomic) CGSize minimumSize;

- (id)initWithFrame:(CGRect)frame andMaxPace:(float)pace_;
- (void)setStrokeColor:(UIColor*)color;
- (void)rollingMaxpace:(float)maxPace animated:(BOOL)animated;

@end
