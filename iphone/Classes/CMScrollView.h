//
//  CMScrollView.h
//  Titanium
//
//  Created by Li Jiantang on 17/09/2014.
//
//

#import "TiUIView.h"

#import "EGORefreshTableHeaderView.h"


@interface CMScrollView : TiUIView<EGORefreshTableHeaderDelegate> {
    EGORefreshTableHeaderView *_refreshHeaderView;
    BOOL _isReloading;
}

- (void)injectCMHeaderViewTo:(UIScrollView*)scrollView;
- (UIScrollView*)getScrollView;

- (void)cmScrollViewDidScroll:(UIScrollView *)scrollView_;
- (void)cmScrollViewDidEndDragging:(UIScrollView *)scrollView_ willDecelerate:(BOOL)decelerate;

- (void)cmRefresh;
- (void)cmMarkRefreshFinished;

@end
