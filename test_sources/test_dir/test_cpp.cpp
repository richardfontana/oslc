/*
	<one line to give the library's name and a brief idea of what it does.>
    Copyright (C) <year>  <name of author>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

//---------------------------------------------------------------------------
#include <iostream>
using namespace std;
#pragma hdrstop
#include "Person1.h"
#include < iostream.h >
#include     "Person2.h"
#include     "  Person3.h"
#include     "Person4.h  "
#include     "  Person5.h  "
#include   Person6.h"
#include   "Person7.h

//---------------------------------------------------------------------------

#pragma argsused
TPerson ProcessRegistration();
void DisplayInformation(const TPerson&);
//---------------------------------------------------------------------------
int main(int argc, char* argv[])
{
/*
this 
	is
		block
			comment */
			
    TPerson Employee = ProcessRegistration(); 
    DisplayInformation(Employee);

    return 0;
}
//---------------------------------------------------------------------------
TPerson ProcessRegistration()
{
    char FName[12], LName[12];
    char Addr[40], CT[32], St[30];
    long ZC;

    cout << "Enter personal information\n";
    cout << "First Name: "; cin >> FName;
    cout << "Last Name:  "; cin >> LName;
    cout << "Address:    "; cin >> ws;
    cin.getline(Addr, 40);
    cout << "City:       ";
    cin.getline(CT, 32);
    cout << "State:      ";
    cin.getline(St, 30);
    cout << "Zip Code:   "; cin >> ZC;

    TPerson Pers(FName, LName, Addr, CT, St, ZC);
    return Pers;
}
//---------------------------------------------------------------------------
void DisplayInformation(const TPerson& Pers)
{
    cout << "\nEmployee Identification";
    cout << "\nFull Name: " << Pers.FullName();
    cout << "\nAddress:   " << Pers.getAddress();
    cout << "\nCity:      " << Pers.getCity() << ", "
         << Pers.getState() << " " << Pers.getZIPCode();
}
//---------------------------------------------------------------------------
