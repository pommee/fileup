# fileup

## What is the meaning of this?

fileup is an attempt to search and find files faster than Windows 11.

## Benchmarks

Benchmarks are done two times in both Windows FS (file search) and fileup.  
First time with a 'clean slate', meaning the pc just rebooted and no CPU caching takes place.  
Second time where both already ran one time.

Both are looking for a rather arbitrary named file with the extension `.dll` located deeply in the C: drive.

### Results

| Program    | Fresh start |  Time |
|------------|:-----------:|------:|
| Windows FS |     Yes     | 01:11 |
| Windows FS |     No      | 00:58 |
| fileup     |     Yes     | 00:08 |
| fileup     |     No      | 00:07 |

These are very rough time calculations but fileup has a 887% increased file search speed.