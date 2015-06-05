<?php
/*
      Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
 
define('NL', "\r\n");
include 'test.file';
include 'http://test_web_page.html';
include_once 'once';
require('twice');
require $var_file;
require_once "req_file1.php";
require_once( "req_file2.php" ) ;

$v1 = 'shared';
$v2 = &$v1;
$v3 = &$v2;
$v4 = &$v3;
# unix test whole one row


echo 'before:'.NL; # unix test end of the row
echo 'v1=' . $v1 . NL; # block end now?>or now
echo 'v2=' . $v2 . NL; # block end ?> // new line comment
echo 'v3=' . $v3 . NL;
echo 'v4=' . $v4 . NL;

// detach messy
$detach = $v1;
unset($v1);
$v1 = $detach;

// detach pretty, but slower
eval(detach('$v2'));

$v1 .= '?';
$v2 .= ' no more';
$v3 .= ' sti';
$v4 .= 'll';

/*-
multiple line
-
*/

echo NL.'after:'.NL;
echo 'v1=' . $v1 . NL;
echo 'v2=' . $v2 . NL;
echo 'v3=' . $v3 . NL;
echo 'v4=' . $v4 . NL;

function detach($v) {
   $e = '$detach = ' . $v . ';';
   $e .= 'unset('.$v.');';
   $e .= $v . ' = $detach;';
   return $e;
}
?>

output {
before:
v1=shared
v2=shared
v3=shared
v4=shared

after:
v1=shared?
v2=shared no more
v3=shared still
v4=shared still
}
