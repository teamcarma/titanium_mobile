//
//  RadarWaveLayer.m
//  Carma
//
//  Created by Li Jiantang on 10/10/2014.
//
//

#import "RadarWaveLayer.h"

@implementation RadarWaveLayer

- (id)initWithLayer:(id)layer {
    if( ( self = [super initWithLayer:layer] ) ) {
        if ([layer isKindOfClass:[RadarWaveLayer class]]) {
            self.pace = ((RadarWaveLayer*)layer).pace;
        }
    }
    return self;
}

+ (BOOL)needsDisplayForKey:(NSString*)key {
    if ([key isEqualToString:@"pace"])
        return YES;
    return [super needsDisplayForKey:key];
}

@end