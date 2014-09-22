//
//  CMScrollView.m
//  Titanium
//
//  Created by Li Jiantang on 17/09/2014.
//
//

#import "CMScrollView.h"

@implementation CMScrollView

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
    }
    return self;
}

- (void) dealloc {
    
    RELEASE_TO_NIL(_refreshHeaderView);
	[super dealloc];
    
}

- (void)injectCMHeaderViewTo:(UIScrollView*)scrollView {
    
    if ([TiUtils boolValue:[[self proxy] valueForKey:@"needPullRefresh"]]) {
        EGORefreshTableHeaderView *headView = [[EGORefreshTableHeaderView alloc] initWithFrame:CGRectMake(0.0f, 0.0f - [self bounds].size.height, [self bounds].size.width, [self bounds].size.height)];
        headView.delegate = self;
        [scrollView addSubview:headView];
        _refreshHeaderView = headView;
    }
}

- (UIScrollView*)getScrollView {
    // to be rewrite by subclass
}

#pragma mark -
#pragma mark Carma UIScrollViewDelegate methods

- (void)cmScrollViewDidScroll:(UIScrollView *)scrollView_
{
    if (_refreshHeaderView) {
        [_refreshHeaderView egoRefreshScrollViewDidScroll:scrollView_];
    }
}

- (void)cmScrollViewDidEndDragging:(UIScrollView *)scrollView_ willDecelerate:(BOOL)decelerate
{
    if (_refreshHeaderView) {
        [_refreshHeaderView egoRefreshScrollViewDidEndDragging:scrollView_];
    }
}


#pragma mark -
#pragma mark EGORefreshTableHeaderDelegate Methods

- (void)egoRefreshTableHeaderDidTriggerRefresh:(EGORefreshTableHeaderView*)view {
	
    NSLog(@"Push to refresh triggered!");
    _isReloading = YES;
    [self.proxy fireEvent:@"didTriggeredRefresh" withObject:nil];
    
}

- (BOOL)egoRefreshTableHeaderShouldTriggerRefresh {
    return !!_refreshHeaderView;
}

- (BOOL)egoRefreshTableHeaderDataSourceIsLoading:(EGORefreshTableHeaderView*)view {
	
	return _isReloading; // should return if data source model is reloading
	
}

- (NSDate*)egoRefreshTableHeaderDataSourceLastUpdated:(EGORefreshTableHeaderView*)view {
	
	return [NSDate date]; // should return date data source was last changed
    
}


#pragma mark Carma pull refresh methods

- (void)cmRefresh {
    
}

- (void)cmMarkRefreshFinished {
    if (_isReloading && _refreshHeaderView) {
        _isReloading = NO;
        [_refreshHeaderView egoRefreshScrollViewDataSourceDidFinishedLoading:[self getScrollView]];
    }
}

@end
