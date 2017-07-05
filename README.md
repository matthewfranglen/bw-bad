Supermarkets
============

This is the 'bad' submission for the brandwatch tech test.

I'm releasing this because the tech test this is based on is not used. It will also be pretty obvious if you use this code.

Original Description
--------------------

This is the solution that I am most proud of. This does not resort to trickery
such as Reflection, yet it is able to perfectly duplicate the Random objects
which are used in the supermarket simulation. With the duplicate Random objects
stock can be managed to a precise degree.

The duplication works by observing the values that are produced by the Random
objects. Random objects in Java work from an initial seed. The initial seed
entirely determines the output of the Random object. Values which are produced
by a Random object under observation are collected and then used to filter the
space of potential seeds until a perfect match is found.

This plugin is also a state machine, and it takes quite some time to move into
the interesting phase. This plugin is also the most CPU heavy by a very large
margin. Given these facts I have log output from a previous execution of this
plugin which you can review (stdout.log).
If you wish to execute it I would recommend increasing the actions per second
to at least 100. Collecting 10,000 ticks of data should be sufficient.

To verify that it is correctly looking into the future, it is good to review
the states that this plugin moves through. The state machine for this is the
most complex of the three, as you will shortly see:

GATHER_DATA - this is the state where it is waiting for the Random objects to
be reproducible. This can take several hundred ticks to end (596 in the linked
log).

STABLE - this is the price that is considered "average". When the plugin is in
this state it will purchase stock required to cover a reasonable amount of
trade (actually quite a high amount of 85). It will stock the shop for exactly
two trades only. You can see this by reviewing the stock held and the trades
that are performed (initial clock tick included so you can review this
in the log):

    ClockTick{tick=785}
    Customer{name='Robert Paulson', stuffNeeded=3, stuffReceived=3}
    Sale{amountSold=3, remainingStock=1}

// At this point the next sale will be for 1 unit of stock

    ArrivalNotification{place=Shop{stock=7}, amount=6}

// The sale following that 1 unit sale will be 6 units

    Total Events: 1256
    Customer{name='Robert Paulson', stuffNeeded=1, stuffReceived=1}

// This is the predicted sale of 1 unit

    Sale{amountSold=1, remainingStock=6}
    ArrivalNotification{place=Shop{stock=8}, amount=2}
    Customer{name='Robert Paulson', stuffNeeded=6, stuffReceived=6}

// This is the predicted sale of 6 units

    Sale{amountSold=6, remainingStock=2}

PRE_RISE - this is when the next change in price will cause the price to rise.
Stock will be bought to cover the aniticipated duration of the price spike.
This should allow every sale during the spike to be serviced without having to
buy at the inflated rate. The STABLE state tends to overstock so you may not
notice any orders being placed - feel free to adjust the base stock level in
GoodPlugin.

There is a limit on the number of price changes that the plugin will consider,
and the timing Random objects are not duplicated, so this is not perfect.

RISEN - this is when the price has risen above the stable level, but it is
still possible to make a profit. This will not buy any stock unless the total
available stock drops to critical levels. When such a drop occurs the plugin
reverts to the stable state at the current price (since purchases can be made
and then sold for a profit, which is better than having no stock at all).

PRE_OVERPRICE - this is when the price will rise to an entirely unprofitable
level. The behaviour at this point is like the PRE_RISE state, as it will
attempt to buy stock to cover the overpriced period. This is done now because a
small profit is better than running out of stock when it is unprofitable to
restock.

OVERPRICED - at this point the price is at or above 10/unit. There will be no
purchases of stock, even if all available stock runs out. If the price drops
below the unprofitable level then the plugin will return to the STABLE state
(allowing it to restock).
