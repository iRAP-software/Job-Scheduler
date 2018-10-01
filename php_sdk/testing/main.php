<?php

require_once(__DIR__ . '/../bootstrap.php');

function main()
{
    $numFailed = 0;
    
    $tests = array(
        new TestTaskTimeout(),
        new TestDependencies(),
        new TestSharedDependencies(),
        new TestBlockageRating(),
        new TestPriorities(),
        new TestMemoryUsage(),
        new TestPerformance()
    );

    foreach ($tests as $test)
    {
        print "Running " . get_class($test) . PHP_EOL;
        /* @var $test TestAbstract */
        $wasSuccess = $test->run();
        
        if ($wasSuccess == false)
        {
            $numFailed++;
            echo $test->getErrorMessage() . PHP_EOL;
        }
    }
    
    if ($numFailed == 0)
    {
        print "Congratulations! All tests succeeded." . PHP_EOL;
    }
    else 
    {
        print $numFailed . " tests failed!" . PHP_EOL;
    }
}

main();