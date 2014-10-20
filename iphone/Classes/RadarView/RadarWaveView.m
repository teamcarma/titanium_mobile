//
//  RadarWaveView.m
//  Carma
//
//  Created by Li Jiantang on 10/10/2014.
//
//

#import "RadarWaveView.h"

#import "RadarWaveLayer.h"

@implementation RadarWaveView
@synthesize maximumPace, alphaMaximumPace, alphaPaceThreshold, minimumSize;

+ (Class)layerClass {
    return [RadarWaveLayer class];
}

- (void)dealloc {
    if (strokeColor) {
        [strokeColor release];
        strokeColor = nil;
    }
    free(animationPaces);
    [super dealloc];
}

- (id)init {
    if ((self = [super init])) {
        [self initializeParametersWithMaxPace:2.5];
        return self;
    }
    return nil;
}

- (id)initWithFrame:(CGRect)frame andMaxPace:(float)maxPace {
    if ((self = [super initWithFrame:frame])) {
        [self initializeParametersWithMaxPace:maxPace];
        return self;
    }
    
    return nil;
}

- (void)initializeParametersWithMaxPace:(float)maxPace {
    self.alphaMaximumPace = maxPace;
    self.maximumPace = maxPace;
    self.minimumSize = CGSizeMake(31, 31);
    [self setStrokeColor:[UIColor darkGrayColor]];
    animationPacesLength = 3;
    animationPaces = (AnimationPace*)malloc(animationPacesLength * sizeof(AnimationPace));
    memset(animationPaces, 0, animationPacesLength * sizeof(AnimationPace));
    self.alphaPaceThreshold = alphaMaximumPace/animationPacesLength;
}

- (void)drawRect:(CGRect)rect {
    NSLog(@"%s",__PRETTY_FUNCTION__);
}

- (void)drawLayer:(CALayer *)layer inContext:(CGContextRef)ctx {
    
    RadarWaveLayer *radarLayer = (RadarWaveLayer*)layer;
    
    if (!staticMode) {
        [self stepForward:radarLayer.pace];
    }
    
    for (int i = 0; i < animationPacesLength - staticMode; i++) {
        [self drawForAnimation:animationPaces[i] inContext:ctx];
    }
}

- (void)drawForAnimation:(AnimationPace)animation inContext:(CGContextRef)ctx {
    CGContextSetStrokeColorWithColor(ctx, [self colorWithAlpha:animation.alpha].CGColor);
    CGContextSetLineWidth(ctx, 2.0f);
    CGContextAddEllipseInRect(ctx, [self ellipseFrameByScale:animation.scale]);
    CGContextStrokePath(ctx);
}

- (CGRect)ellipseFrameByScale:(float)scale_ {
    CGRect frame = self.frame;
    float width = minimumSize.width * scale_;
    float height = minimumSize.height * scale_;
    if (width < minimumSize.width) {
        width = minimumSize.width;
    }
    if (height < minimumSize.height) {
        height = minimumSize.height;
    }
    float xOffset = frame.size.width - width;
    float yOffset = frame.size.height - height;
    if (width > height) {
        xOffset += width - height;
        width = height;
    } else {
        yOffset += height - width;
        height = width;
    }
    
    CGRect newFrame = CGRectMake(xOffset/2, yOffset/2, width, height);
    
    if (debug) {
        NSLog(@"frame = %@", NSStringFromCGRect(newFrame));
    }
    
    return newFrame;
}

- (UIColor*)colorWithAlpha:(float)alpha_ {
    float red, green, blue;
    
    if (strokeColor && [strokeColor getRed:&red green:&green blue:&blue alpha:nil]) {
        return [UIColor colorWithRed:red green:green blue:blue alpha:alpha_];
    }
    
    return strokeColor;
}

- (void)stepForward:(float)pace_ {
    float threshold = alphaPaceThreshold;
    
    for (int i = 0; i < animationPacesLength; i++) {
        if (i == 0 || animationPaces[i].pace != 0 || animationPaces[0].pace >= i * threshold) {
            animationPaces[i].pace = pace_ + i * threshold;
            if (animationPaces[i].pace > alphaMaximumPace) {
                animationPaces[i].pace -= alphaMaximumPace;
            }
            [self setPaceForAnimation:animationPaces + i];
        }
        
        if (debug) {
            NSLog(@" animation { alpha = %f, scale = %f, pace = %f } ", animationPaces[i].alpha, animationPaces[i].scale, animationPaces[i].pace);
        }
        
    }
}

- (void)setPaceForAnimation:(AnimationPace*)animationRef {
    //    if ((*animationRef).pace > alphaPaceThreshold) {
    (*animationRef).scale = 1 + (*animationRef).pace/alphaMaximumPace;
    if ((*animationRef).pace < alphaMaximumPace) {
        if ((*animationRef).pace < alphaPaceThreshold) {
            (*animationRef).alpha = (*animationRef).pace/alphaPaceThreshold;
        } else {
            (*animationRef).alpha = (alphaMaximumPace - (*animationRef).pace)/alphaMaximumPace;
        }
        
    } else {
        (*animationRef).alpha = 0;
    }
    //    } else {
    //        (*animationRef).scale = 1;
    //        (*animationRef).alpha = (*animationRef).pace/alphaPaceThreshold;
    //    }
}

- (void)setStrokeColor:(UIColor*)color {
    [color retain];
    [strokeColor release];
    strokeColor = color;
}

- (CABasicAnimation*)createPaceAnimationWithMaxpace:(float)maxPace {
    CABasicAnimation *animation = [CABasicAnimation animationWithKeyPath:@"pace"];
    animation.duration = maxPace;
    animation.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionLinear];
    animation.fromValue = @(0);
    animation.toValue = @(maxPace);
    animation.repeatCount = HUGE_VALF;
    animation.speed = 1.0;
    
    return animation;
}

- (void)rollingMaxpace:(float)maxPace animated:(BOOL)animated {
    staticMode = !animated;
    if (animated) {
        CABasicAnimation *animation = [self createPaceAnimationWithMaxpace:maxPace];
        [self.layer addAnimation:animation forKey:@"RadarWaveAnimation"];
    } else {
        animationPaces[0].scale = 1.089;
        animationPaces[0].alpha = 1.0;
        
        animationPaces[1].scale = 1.45;
        animationPaces[1].alpha = 1.0;
        
        [self.layer setValue:@(0.8) forKey:@"pace"];
    }
}

@end